/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Jon Brule <brulejr@gmail.com>
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

import io.github.resilience4j.circuitbreaker.CircuitBreaker

/**
 * Interface for receiving notifications related to MQTT reconnect attempts and failures.
 */
interface MqttReconnectNotifier {

    /**
     * Callback invoked when the state of the MQTT connection changes.
     *
     * @param connectionName The name of the MQTT connection.
     */
    fun onStateChange(connectionName: String, from: CircuitBreaker.State, to: CircuitBreaker.State) {}

    /**
     * Callback invoked when an attempt to reconnect to the MQTT broker is made.
     *
     * @param connectionName The name of the MQTT connection.
     * @param attempt The current reconnect attempt number.
     * @param cause The cause of the reconnect attempt failure, if any.
     */
    fun onReconnectAttempt(connectionName: String, attempt: Int, cause: Throwable?) {}

    /**
     * Callback invoked when a successful reconnection to the MQTT broker is established.
     *
     * @param connectionName The name of the MQTT connection.
     */
    fun onReconnectSuccess(connectionName: String) {}

    /**
     * Callback invoked when a failed reconnection attempt to the MQTT broker is made.
     *
     * @param connectionName The name of the MQTT connection.
     * @param attempts The total number of reconnect attempts made.
     * @param lastError The last error encountered during reconnect attempts.
     */
    fun onReconnectFailed(connectionName: String, attempts: Int, lastError: Throwable) {}
}

/**
 * No-op implementation of [MqttReconnectNotifier].
 */
class NoopMqttReconnectNotifier : MqttReconnectNotifier