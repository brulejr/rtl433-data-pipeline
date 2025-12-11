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
import io.jrb.labs.commons.service.CrudOutcome
import io.jrb.labs.rtl433dp.events.AbstractPipelineEventConsumer
import io.jrb.labs.rtl433dp.events.PipelineEvent
import io.jrb.labs.rtl433dp.events.PipelineEventBus
import io.jrb.labs.rtl433dp.features.recommendation.service.BucketingService
import io.jrb.labs.rtl433dp.features.recommendation.service.KnownDeviceService
import io.jrb.labs.rtl433dp.features.recommendation.service.RecommendationService

class RecommendationEventConsumer(
    private val bucketingService: BucketingService,
    private val recommendationService: RecommendationService,
    private val knownDeviceService: KnownDeviceService,
    private val eventBus: PipelineEventBus,
    systemEventBus: SystemEventBus
) : AbstractPipelineEventConsumer<PipelineEvent.Rtl433DataDeduped>(
    kClass = PipelineEvent.Rtl433DataDeduped::class,
    eventBus = eventBus,
    systemEventBus = systemEventBus
) {

    override suspend fun handleEvent(event: PipelineEvent.Rtl433DataDeduped) {
        val payload = event.data
        val deviceId = payload.id
        val propertiesSample = payload.getProperties()

        when (val response = knownDeviceService.findByFingerprint(event.deviceFingerprint)) {

            // skip over known devices
            is CrudOutcome.Success -> {
                val knownDevice = response.data
                eventBus.publish(PipelineEvent.KnownDevice(
                    source = event.source,
                    data = event.data.copy(
                        name = knownDevice.name,
                        type = knownDevice.type,
                        area = knownDevice.area
                    ),
                    deviceFingerprint = event.deviceFingerprint,
                    modelFingerprint = event.modelFingerprint
                ))
            }

            // handle unknown devices
            else -> {
                val bucketCount = bucketingService.registerObservation(event)
                recommendationService.maybeCreateRecommendation(
                    deviceId = deviceId,
                    model = payload.model,
                    deviceFingerprint = event.deviceFingerprint,
                    modelFingerprint = event.modelFingerprint,
                    bucketCount = bucketCount,
                    propertiesSample = propertiesSample
                )
            }

        }
    }

}