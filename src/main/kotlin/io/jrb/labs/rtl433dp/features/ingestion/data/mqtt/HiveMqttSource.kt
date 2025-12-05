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

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import io.jrb.labs.rtl433dp.features.ingestion.data.Source
import io.jrb.labs.rtl433dp.features.ingestion.data.SourceType
import reactor.core.Disposable
import reactor.core.publisher.Flux

class HiveMqttSource(
    private val datafill: MqttSourceDatafill
) : Source {

    private val _mqttClient: Mqtt3AsyncClient = MqttClient.builder()
        .useMqttVersion3()
        .identifier(datafill.clientId)
        .serverHost(datafill.host)
        .serverPort(datafill.port)
        .buildAsync()

    override val name: String
        get() = datafill.name

    override val topic: String
        get() = datafill.topic ?: DEFAULT_TOPIC

    override val type: SourceType = SourceType.MQTT

    override fun connect() {
        _mqttClient.connect()
    }

    override fun disconnect() {
        _mqttClient.disconnect()
    }

    override fun subscribe(topic: String, handler: (String) -> Unit): Disposable {
        return Flux.create{ sink ->
            _mqttClient.subscribeWith()
                .topicFilter(topic)
                .qos(MqttQos.AT_MOST_ONCE)
                .callback { publish ->
                    val payload = String(publish.payloadAsBytes)
                    sink.next(payload)
                }
                .send()
        }.subscribe(handler)
    }

    companion object {
        const val DEFAULT_TOPIC = "#/"
    }

}