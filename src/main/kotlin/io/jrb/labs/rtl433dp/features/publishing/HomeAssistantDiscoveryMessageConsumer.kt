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

package io.jrb.labs.rtl433dp.features.publishing

import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.metrics.FeatureMetrics
import io.jrb.labs.rtl433dp.events.AbstractPipelineEventConsumer
import io.jrb.labs.rtl433dp.events.PipelineEvent
import io.jrb.labs.rtl433dp.events.PipelineEventBus
import io.jrb.labs.rtl433dp.features.FeatureDescriptors.FINGERPRINT
import io.jrb.labs.rtl433dp.features.publishing.service.PublishingService

class HomeAssistantDiscoveryMessageConsumer(
    private val publishingService: PublishingService,
    private val featureMetrics: FeatureMetrics,
    eventBus: PipelineEventBus,
    systemEventBus: SystemEventBus
) : AbstractPipelineEventConsumer<PipelineEvent.HomeAssistantDiscoveryMessage>(
    kClass = PipelineEvent.HomeAssistantDiscoveryMessage::class,
    eventBus = eventBus,
    systemEventBus = systemEventBus
) {

    private val receivedCounter = featureMetrics.eventCounter("received")
    private val errorCounter = featureMetrics.errorCounter("publishing")

    override suspend fun handleEvent(event: PipelineEvent.HomeAssistantDiscoveryMessage) {
        featureMetrics.processingTimer(FINGERPRINT.featureId) {
            try {
                publishingService.publish(event.topic, event.message)
            } catch(e: Exception) {
                errorCounter.increment()
                log.error("Error while processing event for publishing {}", event, e)
            } finally {
                receivedCounter.increment()
            }
        }
    }

}