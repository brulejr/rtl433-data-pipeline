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

package io.jrb.labs.rtl433dp.features.model

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.rtl433dp.events.PipelineEventBus
import io.jrb.labs.rtl433dp.features.model.entities.ModelEntity
import io.jrb.labs.rtl433dp.features.model.repository.ModelRepository
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations

@Configuration
@ConditionalOnProperty(prefix = "application.model", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class ModelConfiguration(
    private val mongoTemplate: ReactiveMongoTemplate
) {

    @Bean
    fun modelService(
        modelRepository: ModelRepository,
        objectMapper: ObjectMapper,
        systemEventBus: SystemEventBus
    ) : ModelService {
        return ModelService(modelRepository, objectMapper, systemEventBus)
    }

    @Bean
    fun modelPipelineEventConsumer(
        modelService: ModelService,
        eventBus: PipelineEventBus,
        systemEventBus: SystemEventBus
    ) : ModelPipelineEventConsumer {
        return ModelPipelineEventConsumer(modelService, eventBus, systemEventBus)
    }

    @PostConstruct
    fun initIndexes() {
        val indexOps: ReactiveIndexOperations = mongoTemplate.indexOps(ModelEntity::class.java)

        val indexes = Index()
            .on("model", Sort.Direction.ASC)
            .unique()

        // trigger the reactive index creation (must subscribe)
        indexOps.createIndex(indexes)
            .subscribe { idx -> println("✅ Ensured index created: $idx") }
    }

}