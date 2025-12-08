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

package io.jrb.labs.rtl433dp.features.model.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.service.ControllableService
import io.jrb.labs.commons.service.CrudOutcome
import io.jrb.labs.commons.util.RefLock
import io.jrb.labs.rtl433dp.events.PipelineEvent
import io.jrb.labs.rtl433dp.features.model.entity.ModelEntity
import io.jrb.labs.rtl433dp.features.model.repository.ModelRepository
import io.jrb.labs.rtl433dp.features.model.resource.ModelResource
import io.jrb.labs.rtl433dp.features.model.resource.Rtl433Search
import io.jrb.labs.rtl433dp.features.model.resource.SensorsUpdateRequest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.sync.withLock
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

class ModelService(
    private val modelRepository: ModelRepository,
    private val objectMapper: ObjectMapper,
    systemEventBus: SystemEventBus
) : ControllableService(systemEventBus) {

    private val locks = ConcurrentHashMap<String, RefLock>()

    suspend fun findModelResource(modelName: String, fingerprint: String): CrudOutcome<ModelResource> {
        return try {
            val resource = modelRepository.findByModelAndFingerprint(modelName, fingerprint)
                .map { it.toModelResource(objectMapper) }
                .awaitFirst()
            if (resource == null) {
                CrudOutcome.NotFound(modelName)
            } else {
                CrudOutcome.Success(resource)
            }
        } catch (e: Exception) {
            CrudOutcome.Error("Failed to find model resource", e)
        }
    }

    suspend fun isModelRecognized(fingerprint: String): Boolean {
        return try {
            val model = modelRepository.findByFingerprint(fingerprint).awaitFirst()
            if (model != null) {
                model.sensors != null && model.sensors.isNotEmpty()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun searchModelResources(rtl433Search: Rtl433Search): CrudOutcome<List<ModelResource>> {
        return try {
            val resources = modelRepository.search(rtl433Search)
                .map { it.toModelResource(objectMapper) }
                .collectList()
                .awaitSingleOrNull() ?: emptyList()
            CrudOutcome.Success(resources)
        } catch (e: Exception) {
            CrudOutcome.Error("Failed to search model resources", e)
        }
    }

    suspend fun processEvent(event: PipelineEvent.Rtl433DataFingerprinted): ModelResource {
        val modelName = event.data.model
        val modelFingerprint = event.modelFingerprint
        val modelStructure = event.modelStructure

        val key = lockKey(modelName, modelFingerprint)
        val refLock = locks.computeIfAbsent(key) { RefLock() }
        refLock.refCount.incrementAndGet()

        try {
            return refLock.mutex.withLock {
                modelRepository.findByModelAndFingerprint(modelName, modelFingerprint)
                    .flatMap { existing ->
                        // exact fingerprint already exists — return as-is
                        Mono.just(existing.toModelResource(objectMapper))
                    }
                    .switchIfEmpty(
                        // new fingerprint for this model → insert
                        Mono.fromSupplier {
                            ModelEntity(
                                source = event.source.toString(),
                                model = modelName,
                                jsonStructure = modelStructure,
                                fingerprint = modelFingerprint
                            ).withCreateInfo()
                        }
                            .flatMap { toInsert -> modelRepository.save(toInsert) }
                            .map { saved -> saved.toModelResource(objectMapper) }
                    )
                    .awaitSingle()
            }
        } finally {
            if (refLock.refCount.decrementAndGet() == 0) {
                locks.remove(key, refLock)
            }
        }
    }

    suspend fun retrieveModelResources(): CrudOutcome<List<ModelResource>> {
        return try {
            val resources = modelRepository.findAll()
                .map { it.toModelResource(objectMapper) }
                .collectList()
                .awaitSingleOrNull() ?: emptyList()
            CrudOutcome.Success(resources)
        } catch (e: Exception) {
            CrudOutcome.Error("Failed to retrieve model resources", e)
        }
    }

    suspend fun updateSensors(modelName: String, fingerprint: String, request: SensorsUpdateRequest): CrudOutcome<ModelResource> {
        return try {
            val updatedModel = modelRepository.findByModelAndFingerprint(modelName, fingerprint)
                .map { it.copy(
                    category = request.category,
                    sensors = request.sensors?.map { it.toSensorMapping() }
                ) }
                .flatMap { modelRepository.save(it) }
                .map { it.toModelResource(objectMapper) }
                .awaitFirst()
            if (updatedModel == null) {
                CrudOutcome.NotFound(modelName)
            } else {
                CrudOutcome.Success(updatedModel)
            }
        } catch (e: Exception) {
            CrudOutcome.Error("Failed to find model resource", e)
        }
    }

    private fun lockKey(model: String, fingerprint: String) = "$model::$fingerprint"

}