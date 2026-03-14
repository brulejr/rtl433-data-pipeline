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

package io.jrb.labs.rtl433dp.features.publishing.data.mqtt

import com.hivemq.client.mqtt.exceptions.MqttClientStateException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.jrb.labs.commons.mqtt.HiveMqttClient
import io.jrb.labs.commons.mqtt.MqttReconnectSupervisor
import io.jrb.labs.rtl433dp.features.publishing.data.Target
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

class HiveMqttTarget(
    private val datafill: MqttTargetDatafill,
    private val reconnectSupervisor: MqttReconnectSupervisor,
) : Target {

    private val log = LoggerFactory.getLogger(HiveMqttTarget::class.java)
    private val mqttClient = HiveMqttClient(datafill)

    override val name: String get() = datafill.name
    override val type: String get() = "MQTT"

    override fun connect() {
        // Intentionally no-op. We connect lazily on publish and on reconnect needs.
        // This avoids racing startup connect with first publish connect.
        log.info("MQTT[{}] target connect is lazy; skipping eager connect()", name)
    }

    override fun disconnect() {
        mqttClient.disconnect()
    }

    override fun publish(topic: String, message: String) {
        val connectionName = datafill.name
        val payload = message.toByteArray()

        fun connect(): Mono<Void> =
            mqttClient.connectMono()
                .onErrorResume { e ->
                    // Critical: do NOT treat this as a failure or you’ll trip Retry/CB.
                    if (isAlreadyConnectedOrConnecting(e)) Mono.empty() else Mono.error(e)
                }

        fun doPublish(): Mono<Void> =
            mqttClient.publishMono(topic, payload)

        val chain =
            reconnectSupervisor.ensureConnected(connectionName) { connect() }
                .then(doPublish())
                .onErrorResume { e ->
                    when {
                        isNotConnected(e) -> {
                            log.warn(
                                "MQTT[{}] publish failed (not connected); reconnecting then retrying. topic={} err={}",
                                connectionName, topic, e.toString()
                            )
                            reconnectSupervisor.ensureConnected(connectionName) { connect() }
                                .then(doPublish())
                        }
                        e is CallNotPermittedException -> {
                            // Circuit breaker is OPEN; don’t spam reconnect attempts here.
                            log.warn(
                                "MQTT[{}] connect circuit breaker is OPEN; dropping publish for now. topic={}",
                                connectionName, topic
                            )
                            Mono.error(e)
                        }
                        else -> Mono.error(e)
                    }
                }
                // Bounded retry for transient publish failures (NOT connect failures).
                .retryWhen(
                    Retry.backoff(2, Duration.ofMillis(250))
                        .maxBackoff(Duration.ofSeconds(2))
                        .filter { err -> !isAlreadyConnectedOrConnecting(err) && err !is CallNotPermittedException }
                )

        // Fire-and-forget (Target.publish is Unit)
        chain.subscribe(
            { /* success */ },
            { e -> log.error("MQTT[{}] publish ultimately failed. topic={}", connectionName, topic, e) }
        )
    }

    private fun isNotConnected(e: Throwable): Boolean {
        val ex = unwrap(e)
        return ex is MqttClientStateException &&
                (ex.message?.contains("not connected", ignoreCase = true) == true)
    }

    private fun isAlreadyConnectedOrConnecting(e: Throwable): Boolean {
        val ex = unwrap(e)
        return ex is MqttClientStateException &&
                (
                        ex.message?.contains("already connected", ignoreCase = true) == true ||
                                ex.message?.contains("already connected or connecting", ignoreCase = true) == true ||
                                ex.message?.contains("connecting", ignoreCase = true) == true
                        )
    }

    private fun unwrap(e: Throwable): Throwable {
        var cur: Throwable = e
        while (cur.cause != null && cur !== cur.cause) {
            cur = cur.cause!!
        }
        return cur
    }
}