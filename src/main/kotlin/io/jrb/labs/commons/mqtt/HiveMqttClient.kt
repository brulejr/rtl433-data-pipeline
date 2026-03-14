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
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

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

    val client: Mqtt3AsyncClient = MqttClient.builder()
        .useMqttVersion3()
        .identifier(datafill.clientId)
        .serverHost(datafill.host)
        .serverPort(datafill.port)
        .addDisconnectedListener { ctx ->
            // Works across HiveMQ client versions where `cause` is not Optional<Throwable>
            val causeObj: Any? = runCatching { ctx.cause }.getOrNull()

            // If it's actually a Throwable, great; otherwise we just keep it for logging
            val cause: Throwable? = causeObj as? Throwable

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

    fun connectAsync(): CompletableFuture<Void> {
        val future = if (datafill.username != null && datafill.password != null) {
            client.connectWith()
                .simpleAuth()
                .username(datafill.username!!)
                .password(datafill.password!!.toByteArray())
                .applySimpleAuth()
                .send()
        } else {
            client.connect()
        }

        // Normalize to CompletableFuture<Void> for Reactor composition
        val cf = CompletableFuture<Void>()
        future.whenComplete { _, t ->
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

    fun connectMono(): Mono<Void> = Mono.fromFuture(connectAsync())

    fun disconnect() {
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
                    log.error("MQTT publish failed [topic={}]: {}", topic, t.message, t)
                }
            }
    }

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
}