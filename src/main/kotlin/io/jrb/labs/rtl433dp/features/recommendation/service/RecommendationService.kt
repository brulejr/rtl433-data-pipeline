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

import io.jrb.labs.commons.logging.LoggerDelegate
import io.jrb.labs.commons.service.CrudOutcome
import io.jrb.labs.rtl433dp.features.recommendation.RecommendationDatafill
import io.jrb.labs.rtl433dp.features.recommendation.entity.Recommendation
import io.jrb.labs.rtl433dp.features.recommendation.repository.RecommendationRepository
import io.jrb.labs.rtl433dp.features.recommendation.resource.RecommendationResource
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import reactor.core.publisher.Mono
import java.time.Instant

class RecommendationService(
    private val repository: RecommendationRepository,
    private val datafill: RecommendationDatafill
) {

    private val log by LoggerDelegate()

    suspend fun maybeCreateRecommendation(
        deviceId: String,
        model: String,
        deviceFingerprint: String,
        modelFingerprint: String,
        bucketCount: Long,
        propertiesSample: Map<String, Any?>
    ): Recommendation? {

        // Frequency gate – keep your existing threshold behavior
        if (bucketCount < datafill.bucketCountThreshold) return null

        val now = Instant.now()
        val existing = repository.findByDeviceFingerprint(deviceFingerprint).awaitFirstOrNull()

        // --- NEW: extract signal strength from the sample ---
        val signalStrengthDbm = extractSignalStrength(propertiesSample)

        // --- NEW: compute weight from frequency + signal strength ---
        val weight = computeWeight(
            bucketCount = bucketCount,
            bucketCountThreshold = datafill.bucketCountThreshold,
            signalStrengthDbm = signalStrengthDbm
        )

        val recommendation = if (existing == null) {
            Recommendation(
                id = null,
                deviceId = deviceId,
                model = model,
                deviceFingerprint = deviceFingerprint,
                modelFingerprint = modelFingerprint,
                firstSeen = now,
                lastSeen = now,
                bucketCount = bucketCount,
                propertiesSample = propertiesSample,
                signalStrengthDbm = signalStrengthDbm,
                weight = weight
            )
        } else {
            existing.copy(
                lastSeen = now,
                bucketCount = bucketCount,
                propertiesSample = propertiesSample,
                signalStrengthDbm = signalStrengthDbm,
                weight = weight
            )
        }

        log.info(
            "Recommendation -> model='{}', id='{}', bucketCount={}, signalStrengthDbm={}, weight={}",
            recommendation.model,
            recommendation.deviceId,
            recommendation.bucketCount,
            recommendation.signalStrengthDbm,
            recommendation.weight
        )

        return repository.save(recommendation).awaitFirstOrNull()
    }

    fun findByDeviceFingerprint(deviceFingerprint: String): Mono<Recommendation> {
        return repository.findByDeviceFingerprint(deviceFingerprint)
    }

    suspend fun listCandidates(): CrudOutcome<List<RecommendationResource>> {
        return try {
            val resources = repository.findAllByPromotedIsFalse()
                .map { it.toRecommendationResource() }
                .collectList()
                .awaitSingleOrNull()
                ?.sortedByDescending { it.weight }        // 👈 sort by weight, highest first
                ?: emptyList()
            CrudOutcome.Success(resources)
        } catch (e: Exception) {
            CrudOutcome.Error("Failed to retrieve recommendation candidates", e)
        }
    }

    /**
     * Try common rtl_433 RSSI/signal field names.
     * Adjust keys here if your payload uses different names.
     */
    private fun extractSignalStrength(properties: Map<String, Any?>): Double? {
        return listOf("rssi", "signal", "signal_dbm", "snr")
            .firstNotNullOfOrNull { key -> (properties[key] as? Number)?.toDouble() }
    }

    /**
     * Compute the recommendation weight from both:
     *  - frequency (bucketCount relative to threshold)
     *  - signal strength (RSSI)
     *
     * Result is a "weight factor" in [0.6, 1.4] that can be used to compare candidates.
     */
    private fun computeWeight(
        bucketCount: Long,
        bucketCountThreshold: Long,
        signalStrengthDbm: Double?
    ): Double {
        // --- Frequency normalization ---
        // bucketCountThreshold -> 0.5
        // 2 * bucketCountThreshold or more -> 1.0
        val freqRatio = bucketCount.toDouble() / bucketCountThreshold.toDouble()
        val frequencyNorm = (freqRatio.coerceIn(0.0, 2.0)) / 2.0     // 0.0..1.0

        // --- Signal normalization ---
        // Map RSSI from [-90, -10] dBm to 0.0..1.0
        val signalNorm = when (signalStrengthDbm) {
            null -> 0.5 // neutral if unknown
            else -> {
                val minDbm = -90.0
                val maxDbm = -10.0
                val clipped = signalStrengthDbm.coerceIn(minDbm, maxDbm)
                (clipped - minDbm) / (maxDbm - minDbm)
            }
        }

        // --- Combine frequency + signal ---
        // Bias slightly toward signal strength as a better "is this really local?" indicator.
        val signalWeight = 0.6
        val frequencyWeight = 0.4
        val combinedNorm = (
                signalNorm * signalWeight +
                        frequencyNorm * frequencyWeight
                ) / (signalWeight + frequencyWeight)

        // --- Map combinedNorm into a bounded factor ---
        val factorMin = 0.6
        val factorMax = 1.4
        val factor = factorMin + (factorMax - factorMin) * combinedNorm.coerceIn(0.0, 1.0)

        return factor
    }

}
