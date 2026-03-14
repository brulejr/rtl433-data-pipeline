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
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
import io.github.resilience4j.reactor.retry.RetryOperator
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Supervisor for MQTT reconnect attempts and failures.
 */
class MqttReconnectSupervisor(
    private val notifier: MqttReconnectNotifier,
    private val connectTimeout: Duration = Duration.ofSeconds(10),
    private val retryConfig: RetryConfig = RetryConfig.custom<Any>()
        .maxAttempts(5)
        .waitDuration(Duration.ofSeconds(2))
        .retryExceptions(Throwable::class.java)
        .build(),
    private val cbConfig: CircuitBreakerConfig = CircuitBreakerConfig.custom()
        .failureRateThreshold(50f)
        .slidingWindowSize(10)
        .minimumNumberOfCalls(5)
        .waitDurationInOpenState(Duration.ofSeconds(60))
        .permittedNumberOfCallsInHalfOpenState(2)
        .build(),
) {
    private val log = LoggerFactory.getLogger(MqttReconnectSupervisor::class.java)

    private val breakers = ConcurrentHashMap<String, CircuitBreaker>()
    private val retries = ConcurrentHashMap<String, Retry>()

    /**
     * Ensure the connection is established.
     *
     * @param connectionName The name of the MQTT connection to ensure.
     * @param connectCall The function to call for establishing the connection.
     * @return A Mono that completes when the connection is established or fails after retries.
     */
    fun ensureConnected(connectionName: String, connectCall: () -> Mono<Void>): Mono<Void> {
        val breaker = breakers.computeIfAbsent(connectionName) {
            CircuitBreaker.of("mqtt-$connectionName-connect", cbConfig).also { cb ->
                cb.eventPublisher.onStateTransition { ev ->
                    notifier.onStateChange(connectionName, ev.stateTransition.fromState, ev.stateTransition.toState)
                    log.warn("MQTT[{}] circuit breaker transition: {}", connectionName, ev.stateTransition)
                }
            }
        }

        val retry = retries.computeIfAbsent(connectionName) {
            Retry.of("mqtt-$connectionName-connect", retryConfig)
        }

        var attempt = 0

        return Mono.defer {
            attempt += 1
            notifier.onReconnectAttempt(connectionName, attempt, null)
            connectCall().timeout(connectTimeout)
        }
            .doOnSuccess { notifier.onReconnectSuccess(connectionName) }
            .doOnError { e -> log.warn("MQTT[{}] connect attempt {} failed: {}", connectionName, attempt, e.toString()) }
            .transformDeferred(RetryOperator.of(retry))
            .transformDeferred(CircuitBreakerOperator.of(breaker))
            .doOnError { e -> notifier.onReconnectFailed(connectionName, attempt, e) }
    }

}