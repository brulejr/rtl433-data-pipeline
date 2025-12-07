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
import io.jrb.labs.rtl433dp.events.PipelineEventBus
import io.jrb.labs.rtl433dp.features.recommendation.entity.BucketCount
import io.jrb.labs.rtl433dp.features.recommendation.entity.Recommendation
import io.jrb.labs.rtl433dp.features.recommendation.repository.KnownDeviceRepository
import io.jrb.labs.rtl433dp.features.recommendation.repository.RecommendationRepository
import io.jrb.labs.rtl433dp.features.recommendation.service.BucketingService
import io.jrb.labs.rtl433dp.features.recommendation.service.KnownDeviceService
import io.jrb.labs.rtl433dp.features.recommendation.service.RecommendationService
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index
import java.time.Duration

@Configuration
@ConfigurationPropertiesScan( basePackages = ["io.jrb.labs.rtl433dp.features.recommendation"])
@ConditionalOnProperty(prefix = "application.recommendation", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class RecommendationConfiguration(
    private val mongoTemplate: ReactiveMongoTemplate
) {

    @Bean
    fun recommendationPipelineEventConsumer(
        bucketingService: BucketingService,
        recommendationService: RecommendationService,
        eventBus: PipelineEventBus,
        systemEventBus: SystemEventBus
    ) : RecommendationEventConsumer {
        return RecommendationEventConsumer(bucketingService, recommendationService, eventBus, systemEventBus)
    }

    @Bean
    fun bucketingService(
        mongo: ReactiveMongoTemplate,
        datafill: RecommendationDatafill
    ) : BucketingService {
        return BucketingService(mongo, datafill)
    }

    @Bean
    fun knownDeviceService(repository: KnownDeviceRepository) : KnownDeviceService {
        return KnownDeviceService(repository)
    }

    @Bean
    fun recommendationService(
        repository: RecommendationRepository,
        datafill: RecommendationDatafill
    ) : RecommendationService {
        return RecommendationService(repository, datafill)
    }

    @PostConstruct
    fun initFingerprintIndexes() {
        val index = Index()
            .on("bucketStartEpoch", Sort.Direction.ASC)
            .expire(Duration.ofDays(1))
        mongoTemplate
            .indexOps(BucketCount::class.java)
            .createIndex(index)
            .subscribe { idx -> println("✅ Ensured fingerprint indexes created: $idx") }
    }

    @PostConstruct
    fun initRecommendationIndexes() {
        val index = Index()
            .on("fingerprint", Sort.Direction.ASC)
            .unique()
        mongoTemplate
            .indexOps(Recommendation::class.java)
            .createIndex(index)
            .subscribe { idx -> println("✅ Ensured recommendation indexes created: $idx") }
    }

}