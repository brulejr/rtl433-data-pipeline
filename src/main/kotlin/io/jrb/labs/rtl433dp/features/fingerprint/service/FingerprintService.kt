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
import io.jrb.labs.rtl433dp.types.Fingerprint
import io.jrb.labs.rtl433dp.types.Rtl433Data
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class FingerprintService(
    private val objectMapper: ObjectMapper,
    systemEventBus: SystemEventBus
) : ControllableService(systemEventBus) {

    fun fingerprint(data: Rtl433Data): Fingerprint {
        val rawJson = objectMapper.writeValueAsString(data)
        val jsonTree = objectMapper.readTree(rawJson)
        val jsonStructure = toJsonStructure(jsonTree)
        val rawJsonStructure = objectMapper.writeValueAsString(jsonStructure)
        return Fingerprint(fingerprint(rawJsonStructure), rawJsonStructure)
    }

    private fun fingerprint(input: String, algorithm: String = "SHA-256"): String {
        val bytes = MessageDigest.getInstance(algorithm).digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
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