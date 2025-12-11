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

/**
 * Thin wrapper around HiveMQ's Mqtt3AsyncClient to centralize connection setup
 * and (optionally) basic publish/subscribe helpers.
 *
 * This is meant to be used via composition from higher-level classes like
 * HiveMqttTarget and HiveMqttSource, not via inheritance.
 */
class HiveMqttClient(private val datafill: HiveMqttDatafill) {

    private val log = LoggerFactory.getLogger(HiveMqttClient::class.java)

    val client: Mqtt3AsyncClient = MqttClient.builder()
        .useMqttVersion3()
        .identifier(datafill.clientId)
        .serverHost(datafill.host)
        .serverPort(datafill.port)
        .buildAsync()

    fun connect() {
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

        future.whenComplete { _, t ->
            if (t != null) {
                log.error(
                    "MQTT connect failed [clientId={}, host={}, port={}]: {}",
                    datafill.clientId, datafill.host, datafill.port, t.message, t
                )
            }
        }
    }

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

    /**
     * Optional helper for publishing. Target can still use the raw client if preferred.
     */
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
                    log.error(
                        "MQTT publish failed [topic={}]: {}",
                        topic, t.message, t
                    )
                }
            }
    }

    /**
     * Optional helper for subscribing: calls the callback with the raw payload bytes.
     * Higher-level code can wrap this in Reactor/Flux, etc.
     */
    fun subscribe(
        topicFilter: String,
        qos: MqttQos = MqttQos.AT_MOST_ONCE,
        callback: (ByteArray) -> Unit
    ) {
        client.subscribeWith()
            .topicFilter(topicFilter)
            .qos(qos)
            .callback { publish ->
                callback(publish.payloadAsBytes)
            }
            .send()
            .whenComplete { _, t ->
                if (t != null) {
                    log.error(
                        "MQTT subscribe failed [topicFilter={}]: {}",
                        topicFilter, t.message, t
                    )
                }
            }
    }

}