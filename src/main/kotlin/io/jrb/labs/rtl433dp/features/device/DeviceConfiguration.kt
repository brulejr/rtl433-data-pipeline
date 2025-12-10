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

package io.jrb.labs.rtl433dp.features.device

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.rtl433dp.events.PipelineEventBus
import io.jrb.labs.rtl433dp.features.device.service.DeviceService
import io.jrb.labs.rtl433dp.features.model.service.ModelService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationPropertiesScan( basePackages = ["io.jrb.labs.rtl433dp.features.device"])
@ConditionalOnProperty(prefix = "application.device", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class DeviceConfiguration {

    @Bean
    fun deviceEventConsumer(
        deviceService: DeviceService,
        eventBus: PipelineEventBus,
        systemEventBus: SystemEventBus
    ) = DeviceEventConsumer(deviceService, eventBus, systemEventBus)

    @Bean
    fun deviceService(
        datafill: DeviceDatafill,
        objectMapper: ObjectMapper,
        modelService: ModelService,
        eventBus: PipelineEventBus,
        systemEventBus: SystemEventBus
    ) = DeviceService(datafill, objectMapper, modelService, eventBus, systemEventBus)

}