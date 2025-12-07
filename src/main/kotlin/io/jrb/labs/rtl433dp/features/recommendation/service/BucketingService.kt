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
package io.jrb.labs.rtl433dp.features.recommendation.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.reactivecircus.cache4k.Cache
import io.jrb.labs.commons.logging.LoggerDelegate
import io.jrb.labs.rtl433dp.features.recommendation.RecommendationDatafill
import io.jrb.labs.rtl433dp.features.recommendation.entity.FingerprintCount
import io.jrb.labs.rtl433dp.types.Rtl433Data
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class BucketingService(
    private val mongoTemplate: ReactiveMongoTemplate,
    private val datafill: RecommendationDatafill
) {
    private val log by LoggerDelegate()

    private val mapper: ObjectMapper = jacksonObjectMapper()

    private val cache = Cache.Builder<String, Boolean>()
        .expireAfterWrite(datafill.dedupeCacheTtlMilliseconds.toDuration(DurationUnit.MILLISECONDS))
        .maximumCacheSize(datafill.dedupeCacheMaxSize)
        .build()

    /**
     * Register a single observation. Returns the new bucket count (after increment).
     * Uses in-memory cache to suppress frequent DB writes for the same fingerprint within TTL.
     */
    suspend fun registerObservation(data: Rtl433Data): Pair<String, Long> {
        val now = Instant.now()
        val fingerprint = fingerprintFor(data)

        val bucketStart = bucketStartEpochMinutes(now, datafill.bucketDurationMinutes)
        val key = "$fingerprint#$bucketStart"

        val cached = cache.get(key) ?: false
        val count =  when (cached) {
            true -> {
                mongoTemplate.findById(key, FingerprintCount::class.java).awaitFirstOrNull()?.count ?: 0L
            }
            false -> {
                cache.put(key, true)
                val q = Query.query(Criteria.where("_id").`is`(key))
                val update = Update()
                    .inc("count", 1)
                    .setOnInsert("fingerprint", fingerprint)
                    .setOnInsert("bucketStartEpoch", bucketStart)

                mongoTemplate.upsert(q, update, FingerprintCount::class.java).awaitFirstOrNull()
                mongoTemplate.findById(key, FingerprintCount::class.java).awaitFirstOrNull()?.count ?: 1L
            }
        }

        log.info("Observation -> dupe = {}, model = {}, id = {}, fingerprint='{}', bucketStart='{}', count='{}'",
            cached, data.model, data.id, fingerprint, bucketStart, count
        )

        return Pair(fingerprint, count)
    }

    fun bucketStartEpochMinutes(now: Instant, bucketMinutes: Long): Long {
        val epochMin = now.epochSecond / 60
        val bucket = (epochMin / bucketMinutes) * bucketMinutes
        return bucket
    }

    fun fingerprintFor(data: Rtl433Data): String {
        val base = mapOf(
            "model" to data.model,
            "deviceId" to data.id
        )
        val json = mapper.writeValueAsString(base)
        log.info("JSON for fingerprint: {}", json)
        val digest = MessageDigest.getInstance("SHA-256").digest(json.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

}
