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

package io.jrb.labs.rtl433dp.features.fingerprint.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.service.ControllableService
import io.jrb.labs.rtl433dp.features.fingerprint.FingerprintDatafill
import io.jrb.labs.rtl433dp.types.Fingerprint
import io.jrb.labs.rtl433dp.types.Rtl433Data
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class FingerprintService(
    private val datafill: FingerprintDatafill,
    private val objectMapper: ObjectMapper,
    systemEventBus: SystemEventBus
) : ControllableService(systemEventBus) {

    private val log = LoggerFactory.getLogger(FingerprintService::class.java)

    fun fingerprint(data: Rtl433Data): Fingerprint {
        val rawJson = objectMapper.writeValueAsString(data)
        val eventFingerprint = fingerprintHash(rawJson)
        val deviceFingerprint = deviceFingerprint(data)
        val modelStructure = modelStructure(rawJson)
        val modelFingerprint = fingerprintHash(modelStructure)

        log.info("Fingerprint -> model = {}, id = {}, eventFingerprint={}, deviceFingerprint='{}', modelFingerprint='{}', modelStructure='{}'",
            data.model, data.id, eventFingerprint, deviceFingerprint, modelFingerprint, modelStructure
        )

        return Fingerprint(
            eventFingerprint,
            deviceFingerprint,
            modelFingerprint,
            modelStructure
        )
    }

    private fun deviceFingerprint(data: Rtl433Data): String {
        val base = mapOf(
            "model" to data.model,
            "deviceId" to data.id
        )
        val json = objectMapper.writeValueAsString(base)
        return fingerprintHash(json)
    }

    private fun fingerprintHash(input: String, algorithm: String = "SHA-256"): String {
        val bytes = MessageDigest.getInstance(algorithm)
            .digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun modelStructure(rawJson: String): String {
        val jsonTree = objectMapper.readTree(rawJson)

        val filteredTree =
            if (datafill.enabled && datafill.excludedFields.isNotEmpty()) {
                excludeByFieldName(jsonTree)
            } else {
                jsonTree
            }

        val jsonStructure = toJsonStructure(filteredTree)
        return objectMapper.writeValueAsString(jsonStructure)
    }

    /**
     * Excludes any object field whose name matches excludedFields, anywhere in the JSON tree.
     *
     * - No hierarchy/path matching.
     * - Name-only matching.
     * - Parent objects are preserved even if they become empty
     *   (structure will reflect that, which is usually fine for fingerprinting).
     */
    internal fun excludeByFieldName(root: JsonNode): JsonNode {
        val excludes = datafill.excludedFields

        val presentNames = collectAllFieldNames(root)
        val appliedExcludes = excludes.intersect(presentNames)
        val missingExcludes = excludes - presentNames

        fun filterNode(node: JsonNode): JsonNode {
            return when {
                node.isObject -> {
                    val out = objectMapper.createObjectNode()

                    node.fieldNames().asSequence().forEach { fieldName ->
                        if (fieldName in excludes) {
                            // skip entirely
                            return@forEach
                        }
                        val child = node[fieldName]
                        out.set<JsonNode>(fieldName, filterNode(child))
                    }

                    out
                }
                node.isArray -> node
                else -> node
            }
        }

        val filtered = filterNode(root)

        if (log.isDebugEnabled) {
            log.debug(
                "Fingerprint exclude-only summary: presentFieldNames={}, excludesRequested={}",
                presentNames.size, excludes.size
            )
            log.debug(
                "Fingerprint excludes: applied={}, missing={}",
                appliedExcludes.sortedSafe(),
                missingExcludes.sortedSafe()
            )
        }

        return filtered
    }

    /**
     * Collect all object field names at any depth.
     */
    internal fun collectAllFieldNames(root: JsonNode): Set<String> {
        val names = linkedSetOf<String>()

        fun walk(node: JsonNode) {
            when {
                node.isObject -> {
                    node.fieldNames().forEachRemaining { name ->
                        names += name
                        walk(node[name])
                    }
                }
                node.isArray -> node.forEach { walk(it) }
                else -> { /* primitives */ }
            }
        }

        walk(root)
        return names
    }

    private fun toJsonStructure(node: JsonNode): JsonNode {
        return when {
            node.isObject -> {
                val sortedNode = objectMapper.createObjectNode()
                node.fieldNames().asSequence().sorted().forEach { key ->
                    sortedNode.set<JsonNode>(key, toJsonStructure(node[key]))
                }
                sortedNode
            }
            node.isArray -> JsonNodeFactory.instance.textNode("array")
            node.isTextual -> JsonNodeFactory.instance.textNode("string")
            node.isNumber -> JsonNodeFactory.instance.textNode("number")
            node.isBoolean -> JsonNodeFactory.instance.textNode("boolean")
            node.isNull -> JsonNodeFactory.instance.textNode("null")
            else -> JsonNodeFactory.instance.textNode("unknown")
        }
    }

    private fun Set<String>.sortedSafe(max: Int = 50): List<String> =
        this.asSequence().sorted().take(max).toList()

}
