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

package io.jrb.labs.rtl433dp.features.recommendation

import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.rtl433dp.events.AbstractPipelineEventConsumer
import io.jrb.labs.rtl433dp.events.PipelineEvent
import io.jrb.labs.rtl433dp.events.PipelineEventBus
import io.jrb.labs.rtl433dp.features.recommendation.service.BucketingService
import io.jrb.labs.rtl433dp.features.recommendation.service.RecommendationService

class RecommendationEventConsumer(
    private val bucketingService: BucketingService,
    private val recommendationService: RecommendationService,
    eventBus: PipelineEventBus,
    systemEventBus: SystemEventBus
) : AbstractPipelineEventConsumer<PipelineEvent.Rtl433DataFingerprinted>(
    kClass = PipelineEvent.Rtl433DataFingerprinted::class,
    eventBus = eventBus,
    systemEventBus = systemEventBus
) {

    override suspend fun handleEvent(event: PipelineEvent.Rtl433DataFingerprinted) {
        val payload = event.data
        val deviceId = payload.id
        val propertiesSample = payload.getProperties()

        val (deviceFingerprint, bucketCount) = bucketingService.registerObservation(event)

        recommendationService.maybeCreateRecommendation(
            fingerprint = deviceFingerprint,
            model = payload.model,
            deviceId = deviceId,
            bucketCount = bucketCount,
            propertiesSample = propertiesSample
        )
    }

}