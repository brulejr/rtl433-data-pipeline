/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Jon Brule <brulejr@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.jrb.labs.commons.mqtt

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.exceptions.MqttClientStateException
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * Thin wrapper around HiveMQ's Mqtt3AsyncClient to centralize connection setup
 * and (optionally) basic publish/subscribe helpers.
 *
 * This is meant to be used via composition from higher-level classes like
 * HiveMqttTarget and HiveMqttSource, not via inheritance.
 */
class HiveMqttClient(private val datafill: HiveMqttDatafill) {

    private val log = LoggerFactory.getLogger(HiveMqttClient::class.java)

    private val disconnectedListeners = CopyOnWriteArrayList<(Throwable?) -> Unit>()

    /**
     * Single-flight connect: concurrent callers share one in-flight future.
     */
    private val inFlightConnect = AtomicReference<CompletableFuture<Void>?>(null)

    val client: Mqtt3AsyncClient = MqttClient.builder()
        .useMqttVersion3()
        .identifier(datafill.clientId)
        .serverHost(datafill.host)
        .serverPort(datafill.port)
        .addDisconnectedListener { ctx ->
            // Works across HiveMQ client versions where `cause` is not Optional<Throwable>
            val causeObj: Any? = runCatching { ctx.cause }.getOrNull()
            val cause: Throwable? = causeObj as? Throwable

            // Allow new connect attempts after any disconnect
            inFlightConnect.set(null)

            // "Client sent DISCONNECT" is usually normal shutdown; keep WARN for now (you can lower later)
            log.warn(
                "MQTT disconnected [clientId={}, host={}, port={}]: {}",
                datafill.clientId,
                datafill.host,
                datafill.port,
                causeObj?.toString() ?: "unknown"
            )

            disconnectedListeners.forEach { it.invoke(cause) }
        }
        .buildAsync()

    fun addDisconnectedListener(listener: (Throwable?) -> Unit) {
        disconnectedListeners.add(listener)
    }

    /**
     * Keep for callers that want a raw future.
     * (Not single-flight; prefer connectMono() for gating.)
     */
    fun connectAsync(): CompletableFuture<Void> {
        val f = connectInternal()
        val cf = CompletableFuture<Void>()
        f.whenComplete { _, t ->
            if (t != null) {
                log.error(
                    "MQTT connect failed [clientId={}, host={}, port={}]: {}",
                    datafill.clientId, datafill.host, datafill.port, t.message, t
                )
                cf.completeExceptionally(t)
            } else {
                cf.complete(null)
            }
        }
        return cf
    }

    /**
     * Single-flight / idempotent connect.
     *
     * - If another connect is in progress, joins it.
     * - If the client says "already connected or connecting", treat as success.
     * - Clears inFlightConnect on completion and on disconnect.
     */
    fun connectMono(): Mono<Void> {
        val existing = inFlightConnect.get()
        if (existing != null) return Mono.fromFuture(existing)

        val gate = CompletableFuture<Void>()
        if (!inFlightConnect.compareAndSet(null, gate)) {
            return Mono.fromFuture(inFlightConnect.get()!!)
        }

        val f = try {
            connectInternal()
        } catch (e: MqttClientStateException) {
            return if (isAlreadyConnectedOrConnecting(e)) {
                // Treat as success; complete gate and clear
                gate.complete(null)
                inFlightConnect.compareAndSet(gate, null)
                Mono.fromFuture(gate)
            } else {
                gate.completeExceptionally(e)
                inFlightConnect.compareAndSet(gate, null)
                Mono.error(e)
            }
        } catch (e: Throwable) {
            gate.completeExceptionally(e)
            inFlightConnect.compareAndSet(gate, null)
            return Mono.error(e)
        }

        f.whenComplete { _, t ->
            if (t != null) gate.completeExceptionally(t) else gate.complete(null)
            inFlightConnect.compareAndSet(gate, null)
        }

        return Mono.fromFuture(gate)
    }

    fun disconnect() {
        // Clear any in-flight marker on explicit disconnect as well.
        inFlightConnect.set(null)

        client.disconnect().whenComplete { _, t ->
            if (t != null) {
                log.warn(
                    "MQTT disconnect failed [clientId={}, host={}, port={}]: {}",
                    datafill.clientId, datafill.host, datafill.port, t.message, t
                )
            }
        }
    }

    fun publish(
        topic: String,
        payload: ByteArray,
        qos: MqttQos = MqttQos.AT_LEAST_ONCE
    ) {
        client.publishWith()
            .topic(topic)
            .payload(payload)
            .qos(qos)
            .send()
            .whenComplete { _, t ->
                if (t != null) {
                    logPublishFailure(topic, t)
                }
            }
    }

    fun publishAsync(
        topic: String,
        payload: ByteArray,
        qos: MqttQos = MqttQos.AT_LEAST_ONCE
    ): CompletableFuture<Void> {
        val f = client.publishWith()
            .topic(topic)
            .payload(payload)
            .qos(qos)
            .send()

        val cf = CompletableFuture<Void>()
        f.whenComplete { _, t ->
            if (t != null) {
                logPublishFailure(topic, t)
                cf.completeExceptionally(t)
            } else {
                cf.complete(null)
            }
        }
        return cf
    }

    fun publishMono(
        topic: String,
        payload: ByteArray,
        qos: MqttQos = MqttQos.AT_LEAST_ONCE
    ): Mono<Void> = Mono.fromFuture(publishAsync(topic, payload, qos))

    fun subscribe(
        topicFilter: String,
        qos: MqttQos = MqttQos.AT_MOST_ONCE,
        callback: (ByteArray) -> Unit
    ) {
        client.subscribeWith()
            .topicFilter(topicFilter)
            .qos(qos)
            .callback { publish -> callback(publish.payloadAsBytes) }
            .send()
            .whenComplete { _, t ->
                if (t != null) {
                    log.error("MQTT subscribe failed [topicFilter={}]: {}", topicFilter, t.message, t)
                }
            }
    }

    // -----------------------
    // Internals
    // -----------------------

    private fun connectInternal(): CompletableFuture<Void> {
        return if (datafill.username != null && datafill.password != null) {
            client.connectWith()
                .simpleAuth()
                .username(datafill.username!!)
                .password(datafill.password!!.toByteArray())
                .applySimpleAuth()
                .send()
                .toVoidFuture()
        } else {
            client.connect()
                .toVoidFuture()
        }
    }

    private fun <T> CompletableFuture<T>.toVoidFuture(): CompletableFuture<Void> {
        val cf = CompletableFuture<Void>()
        this.whenComplete { _, t ->
            if (t != null) cf.completeExceptionally(t) else cf.complete(null)
        }
        return cf
    }

    private fun isAlreadyConnectedOrConnecting(e: MqttClientStateException): Boolean {
        val msg = e.message ?: return false
        return msg.contains("already connected", ignoreCase = true) ||
                msg.contains("already connected or connecting", ignoreCase = true) ||
                msg.contains("connecting", ignoreCase = true)
    }

    private fun isNotConnected(t: Throwable): Boolean {
        return t is MqttClientStateException &&
                (t.message?.contains("not connected", ignoreCase = true) == true)
    }

    private fun logPublishFailure(topic: String, t: Throwable) {
        if (isNotConnected(t)) {
            // In a lazy-connect/reconnect design, this can be expected transiently.
            log.debug("MQTT publish deferred (not connected yet) [topic={}]: {}", topic, t.message)
        } else {
            log.error("MQTT publish failed [topic={}]: {}", topic, t.message, t)
        }
    }
}