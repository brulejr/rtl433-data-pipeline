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

package io.jrb.labs.commons.metrics

import io.jrb.labs.commons.feature.FeatureDescriptor
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.time.Duration

/**
 * A utility class for defining and managing various feature-related metrics.
 *
 * This class simplifies the creation of metrics for monitoring feature-specific
 * states, events, errors, and processing latencies. Metrics are registered to
 * a `MeterRegistry` for integration with monitoring systems.
 *
 * @constructor Creates an instance of `FeatureMetrics`.
 * @param registry The `MeterRegistry` used for registering the metrics.
 * @param featureDescriptor The descriptor containing details about the specific feature being monitored.
 */
class FeatureMetrics(
    private val registry: MeterRegistry,
    private val featureDescriptor: FeatureDescriptor
) {

    /**
     * Creates a counter for tracking the total number of errors observed within a specific feature stage.
     *
     * @param stage The stage of the feature for which errors are being tracked.
     * @return A Counter object for incrementing error metrics related to the specified feature stage.
     */
    fun errorCounter(stage: String): Counter {
        val name = "%s_feature_errors_total".format(featureDescriptor.application)
        return Counter.builder(name)
            .description("Total errors observed by a feature")
            .tag("feature", featureDescriptor.featureId)
            .tag("stage", stage)
            .register(registry)
    }

    /**
     * Creates a counter for tracking the total number of events processed within a specific feature stage.
     *
     * @param stage The stage of the feature for which events are being tracked.
     * @return A Counter object for incrementing event metrics related to the specified feature stage.
     */
    fun eventCounter(stage: String): Counter {
        val name = "%s_feature_events_total".format(featureDescriptor.application)
        return Counter.builder(name)
            .description("Total events processed by a feature")
            .tag("feature", featureDescriptor.featureId)
            .tag("stage", stage)
            .register(registry)
    }

    /**
     * Creates and registers a Gauge metric that tracks the state of a feature.
     *
     * The Gauge reports a value of 0 if the feature state is "down" and 1 if the feature state is "up".
     * Optionally, a stage tag can be added to distinguish the feature's lifecycle stage.
     *
     * @param state A lambda function that returns a boolean indicating the current state of the feature
     *              (true for "up" and false for "down").
     * @param stage An optional parameter specifying the lifecycle stage of the feature (e.g., "discovery").
     *              Defaults to null if not provided.
     * @return A Gauge object that has been registered with the monitoring registry.
     */
    fun featureStateGauge(state: () -> Boolean, stage: String? = null): Gauge {
        val name = "%s_feature_state".format(featureDescriptor.application)
        return Gauge.builder(name) { if (state()) 1 else 0 }
            .description("Feature state (0=down, 1=up)")
            .tag("feature", featureDescriptor.featureId)
            .tag("stage", stage ?: "")
            .register(registry)
    }

    /**
     * Measures the time taken to execute a specific processing stage of a feature.
     *
     * This function tracks the latency of the given processing block by recording
     * its execution duration in a timer metric. The metric includes percentile distribution
     * and a histogram for detailed latency analysis.
     *
     * @param stage The stage of the feature whose processing time is being measured.
     * @param block A suspending lambda representing the processing logic to be timed.
     * @return The result produced by the execution of the provided block.
     */
    suspend fun <T> processingTimer(stage: String, block: suspend () -> T): T {
        val name = "%s_feature_processing_seconds".format(featureDescriptor.application)
        val timer = Timer.builder(name)
            .description("Feature processing latency")
            .tag("feature", featureDescriptor.featureId)
            .tag("stage", stage)
            .publishPercentiles(0.5, 0.9, 0.95, 0.99)
            .publishPercentileHistogram()
            .minimumExpectedValue(Duration.ofMillis(1))
            .maximumExpectedValue(Duration.ofSeconds(5))
            .register(registry)
        val sample = Timer.start(registry)
        try {
            return block()
        } finally {
            sample.stop(timer)
        }
    }

}