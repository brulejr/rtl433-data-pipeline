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
import io.jrb.labs.commons.metrics.FeatureMetricsFactory
import io.jrb.labs.rtl433dp.events.PipelineEventBus
import io.jrb.labs.rtl433dp.features.FeatureDescriptors.CONFIG_PREFIX_PUBLISHING
import io.jrb.labs.rtl433dp.features.FeatureDescriptors.PUBLISHING
import io.jrb.labs.rtl433dp.features.publishing.data.Target
import io.jrb.labs.rtl433dp.features.publishing.data.mqtt.HiveMqttTarget
import io.jrb.labs.rtl433dp.features.publishing.service.PublishingService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationPropertiesScan( basePackages = ["io.jrb.labs.rtl433dp.features.publishing"])
@ConditionalOnProperty(prefix = CONFIG_PREFIX_PUBLISHING, name = ["enabled"], havingValue = "true", matchIfMissing = true)
class PublishingConfiguration {

    @Bean
    fun publishingFeatureMetrics(featureMetricsFactory: FeatureMetricsFactory) =
        featureMetricsFactory.forFeature(PUBLISHING)

    @Bean
    fun homeAssistantDiscoveryMessageConsumer(
        publishingService: PublishingService,
        publishingFeatureMetrics: FeatureMetrics,
        eventBus: PipelineEventBus,
        systemEventBus: SystemEventBus
    ) = HomeAssistantDiscoveryMessageConsumer(publishingService, publishingFeatureMetrics, eventBus, systemEventBus)

    @Bean
    fun homeAssistantSensorMessageConsumer(
        publishingService: PublishingService,
        publishingFeatureMetrics: FeatureMetrics,
        eventBus: PipelineEventBus,
        systemEventBus: SystemEventBus
    ) = HomeAssistantSensorMessageConsumer(publishingService, publishingFeatureMetrics, eventBus, systemEventBus)

    @Bean
    fun publishingService(
        targets: List<Target>,
        systemEventBus: SystemEventBus
    ) : PublishingService {
        return PublishingService(targets, systemEventBus)
    }

    @Bean
    fun targets(datafill: PublishingDatafill): List<Target> {
        return datafill.mqtt.map { target -> HiveMqttTarget(target) }
    }

    @Bean
    fun publishingInfoContributor(datafill: PublishingDatafill) = PublishingInfoContributor(datafill)

}