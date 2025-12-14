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
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.time.Duration

class FeatureMetrics(
    private val registry: MeterRegistry,
    private val featureDescriptor: FeatureDescriptor
) {

    fun eventCounter(event: String): Counter =
        Counter.builder("%s_feature_events_total".format(featureDescriptor.application))
            .description("Total events processed by a feature")
            .tag("feature", featureDescriptor.featureId)
            .tag("event", event)
            .register(registry)

    fun errorCounter(stage: String): Counter =
        Counter.builder("%s_feature_errors_total".format(featureDescriptor.application))
            .description("Total errors observed by a feature")
            .tag("feature", featureDescriptor.featureId)
            .tag("stage", stage)
            .register(registry)

    fun processingTimer(stage: String): Timer =
        Timer.builder("%s_feature_processing_seconds".format(featureDescriptor.application))
            .description("Feature processing latency")
            .tag("feature", featureDescriptor.featureId)
            .tag("stage", stage)
            .publishPercentileHistogram()
            .minimumExpectedValue(Duration.ofMillis(1))
            .maximumExpectedValue(Duration.ofSeconds(5))
            .register(registry)

}