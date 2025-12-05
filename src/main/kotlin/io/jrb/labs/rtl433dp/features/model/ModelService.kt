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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.logging.LoggerDelegate
import io.jrb.labs.commons.service.ControllableService
import io.jrb.labs.commons.service.CrudOutcome
import io.jrb.labs.commons.util.RefLock
import io.jrb.labs.rtl433dp.events.PipelineEvent
import io.jrb.labs.rtl433dp.features.model.entities.ModelEntity
import io.jrb.labs.rtl433dp.features.model.repository.ModelRepository
import io.jrb.labs.rtl433dp.features.model.resources.ModelResource
import io.jrb.labs.rtl433dp.features.model.resources.Rtl433Search
import io.jrb.labs.rtl433dp.features.model.resources.SensorsUpdateRequest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.sync.withLock
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class ModelService(
    private val modelRepository: ModelRepository,
    private val objectMapper: ObjectMapper,
    systemEventBus: SystemEventBus
) : ControllableService(systemEventBus) {

    private val log by LoggerDelegate()

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

    suspend fun processEvent(event: PipelineEvent.Rtl433DataReceived): ModelResource {
        val modelName = event.data.model
        val jsonStructure = extractJsonStructure(event)
        val fingerprint = fingerprint(jsonStructure)

        val key = lockKey(modelName, fingerprint)
        val refLock = locks.computeIfAbsent(key) { RefLock() }
        refLock.refCount.incrementAndGet()

        try {
            return refLock.mutex.withLock {
                modelRepository.findByModelAndFingerprint(modelName, fingerprint)
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
                                jsonStructure = jsonStructure,
                                fingerprint = fingerprint
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

    private fun extractJsonStructure(event: PipelineEvent.Rtl433DataReceived): String {
        val rawJson = objectMapper.writeValueAsString(event)
        val jsonTree = objectMapper.readTree(rawJson)
        val jsonStructure = toJsonStructure(jsonTree)
        log.info("JSON: $rawJson")
        log.info("JSON Structure: $jsonStructure")
        return objectMapper.writeValueAsString(jsonStructure)
    }

    private fun fingerprint(input: String, algorithm: String = "SHA-256"): String {
        val bytes = MessageDigest.getInstance(algorithm).digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun lockKey(model: String, fingerprint: String) = "$model::$fingerprint"

    private fun toJsonStructure(node: JsonNode): JsonNode {
        return when {
            node.isObject -> {
                val sortedNode = objectMapper.createObjectNode()
                node.fieldNames().asSequence().sorted().forEach { key ->
                    sortedNode.set<JsonNode>(key, toJsonStructure(node[key]))
                }
                sortedNode
            }
            node.isArray -> {
                val arrayPlaceholder = JsonNodeFactory.instance.textNode("array") // Represents an array
                arrayPlaceholder
            }
            node.isTextual -> JsonNodeFactory.instance.textNode("string")
            node.isNumber -> JsonNodeFactory.instance.textNode("number")
            node.isBoolean -> JsonNodeFactory.instance.textNode("boolean")
            node.isNull -> JsonNodeFactory.instance.textNode("null")
            else -> JsonNodeFactory.instance.textNode("unknown")
        }
    }

}