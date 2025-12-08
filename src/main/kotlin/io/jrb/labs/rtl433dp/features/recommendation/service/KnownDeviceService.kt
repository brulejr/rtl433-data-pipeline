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

import io.jrb.labs.commons.service.CrudOutcome
import io.jrb.labs.rtl433dp.features.model.service.ModelService
import io.jrb.labs.rtl433dp.features.recommendation.entity.KnownDevice
import io.jrb.labs.rtl433dp.features.recommendation.entity.Recommendation
import io.jrb.labs.rtl433dp.features.recommendation.repository.KnownDeviceRepository
import io.jrb.labs.rtl433dp.features.recommendation.repository.RecommendationRepository
import io.jrb.labs.rtl433dp.features.recommendation.resource.KnownDeviceResource
import io.jrb.labs.rtl433dp.features.recommendation.resource.PromotionRequest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory

class KnownDeviceService(
    private val knownDeviceRepository: KnownDeviceRepository,
    private val recommendationRepository: RecommendationRepository,
    private val modelService: ModelService
) {

    private val log = LoggerFactory.getLogger(KnownDeviceService::class.java)

    suspend fun findByFingerprint(fingerprint: String): CrudOutcome<KnownDeviceResource> {
        return try {
            val resource = knownDeviceRepository.findByFingerprint(fingerprint)
                .map { it.toKnownDeviceResource() }
                .awaitFirst()
            if (resource == null) {
                CrudOutcome.NotFound(fingerprint)
            } else {
                CrudOutcome.Success(resource)
            }
        } catch (e: Exception) {
            CrudOutcome.Error("Failed to find known device resource", e)
        }
    }

    suspend fun retrieveKnownDeviceResources(): CrudOutcome<List<KnownDeviceResource>> {
        return try {
            val resources = knownDeviceRepository.findAll()
                .map { it.toKnownDeviceResource() }
                .collectList()
                .awaitSingleOrNull() ?: emptyList()
            CrudOutcome.Success(resources)
        } catch (e: Exception) {
            CrudOutcome.Error("Failed to known device model resources", e)
        }
    }

    suspend fun promoteRecommendation(promotionRequest: PromotionRequest): CrudOutcome<KnownDeviceResource> {
        return try {
            val deviceFingerprint = promotionRequest.deviceFingerprint
            val knownDevice = findKnownDevice(deviceFingerprint)
            if (knownDevice == null) {
                val recommendation = findRecommendation(deviceFingerprint)
                if (recommendation != null) {
                    if (modelService.isModelRecognized(recommendation.modelFingerprint)) {
                        val entity = KnownDevice(promotionRequest, recommendation)
                        log.info("Known Device -> {}", entity)
                        knownDeviceRepository.save(entity).awaitSingle().toKnownDeviceResource().let(
                            { CrudOutcome.Success(it) }
                        )
                    } else {
                        CrudOutcome.NotFound("Model is not recognized for device fingerprint: $deviceFingerprint")
                    }
                } else {
                    CrudOutcome.NotFound("Recommendation not found for device fingerprint: $deviceFingerprint")
                }
            } else {
                CrudOutcome.Conflict("Device already known for fingerprint: $deviceFingerprint")
            }
        } catch (e: Exception) {
            CrudOutcome.Error("Failed to promote recommendation", e)
        }
    }

    private suspend fun findKnownDevice(fingerprint: String): KnownDevice? {
        return knownDeviceRepository.findByFingerprint(fingerprint).awaitSingleOrNull()
    }

    private suspend fun findRecommendation(fingerprint: String): Recommendation? {
        return recommendationRepository.findByDeviceFingerprint(fingerprint).awaitSingleOrNull()
    }

}