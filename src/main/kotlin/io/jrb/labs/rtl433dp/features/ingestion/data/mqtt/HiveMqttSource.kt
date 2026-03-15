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

package io.jrb.labs.rtl433dp.features.ingestion.data.mqtt

import io.jrb.labs.commons.mqtt.HiveMqttClient
import io.jrb.labs.commons.mqtt.MqttReconnectSupervisor
import io.jrb.labs.rtl433dp.features.ingestion.data.Source
import io.jrb.labs.rtl433dp.features.ingestion.data.SourceType
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.util.retry.Retry
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class HiveMqttSource(
    private val datafill: MqttSourceDatafill,
    private val reconnectSupervisor: MqttReconnectSupervisor, // NEW
) : Source {

    private val mqttClient = HiveMqttClient(datafill)

    override val name: String get() = datafill.name
    override val topic: String get() = datafill.topic ?: DEFAULT_TOPIC
    override val type: SourceType = SourceType.MQTT

    override fun connect() {
        // IngestionService still calls this; safe to keep.
        // Actual resiliency is handled inside subscribe() now.
        mqttClient.connectAsync()
    }

    override fun disconnect() {
        mqttClient.disconnect()
    }

    override fun subscribe(topic: String, handler: (String) -> Unit): Disposable {
        val connectionName = datafill.name

        val flux = Flux.defer {
            // Ensure connected (with retry + circuit breaker) before subscribing
            reconnectSupervisor.ensureConnected(connectionName) { mqttClient.connectMono() }
                .thenMany(
                    Flux.create<String> { sink ->
                        val cancelled = AtomicBoolean(false)

                        val disconnectListener: (Throwable?) -> Unit = { cause ->
                            if (!cancelled.get() && !sink.isCancelled) {
                                sink.error(cause ?: IllegalStateException("MQTT disconnected"))
                            }
                        }

                        mqttClient.addDisconnectedListener(disconnectListener)

                        mqttClient.subscribe(topicFilter = topic) { payloadBytes ->
                            if (!cancelled.get() && !sink.isCancelled) {
                                sink.next(String(payloadBytes))
                            }
                        }

                        sink.onDispose {
                            cancelled.set(true)
                            // No hard unsubscribe here (your wrapper doesn't implement it),
                            // but we stop emitting and let disconnect()/stop handle lifecycle.
                        }
                    }
                )
        }
            // If the disconnect listener errors the Flux, this will resubscribe forever,
            // BUT each re-subscribe must pass through the supervisor (retry+breaker).
            .retryWhen(
                Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                    .maxBackoff(Duration.ofSeconds(30))
            )

        return flux.subscribe(handler)
    }

    companion object {
        const val DEFAULT_TOPIC = "#/"
    }

}