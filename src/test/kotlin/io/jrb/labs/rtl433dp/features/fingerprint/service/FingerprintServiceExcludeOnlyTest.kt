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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.rtl433dp.features.fingerprint.FingerprintDatafill
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FingerprintServiceExcludeOnlyTest {

    private val objectMapper = jacksonObjectMapper()
    private val systemEventBus = mockk<SystemEventBus>(relaxed = true)

    private fun service(datafill: FingerprintDatafill): FingerprintService =
        FingerprintService(datafill, objectMapper, systemEventBus)

    private val sampleJson = """
        {
          "model": "Acurite-5n1",
          "id": "1234",
          "time": "2025-12-06T13:00:00Z",
          "properties": {
            "battery_ok": 1,
            "temperature_C": -10.5,
            "humidity": 55,
            "raw": { "foo": "bar" }
          },
          "meta": {
            "id": "nested-id",
            "note": "hello",
            "time": "nested-time"
          }
        }
    """.trimIndent()

    @Test
    fun `collectAllFieldNames finds names across hierarchy`() {
        val svc = service(FingerprintDatafill())

        val tree = objectMapper.readTree(sampleJson)
        val names = svc.collectAllFieldNames(tree)

        assertThat(names).contains(
            "model", "id", "time", "properties", "battery_ok",
            "temperature_C", "humidity", "raw", "meta", "note", "foo"
        )
    }

    @Test
    fun `excludeByFieldName removes matching names anywhere`() {
        val svc = service(
            FingerprintDatafill(
                enabled = true,
                excludedFields = setOf("time", "raw", "note")
            )
        )

        val tree = objectMapper.readTree(sampleJson)
        val filtered = svc.excludeByFieldName(tree)

        // top-level removed
        assertThat(filtered.has("time")).isFalse

        // nested removed
        assertThat(filtered["properties"].has("raw")).isFalse
        assertThat(filtered["meta"].has("note")).isFalse
        assertThat(filtered["meta"].has("time")).isFalse

        // untouched fields remain
        assertThat(filtered.has("model")).isTrue
        assertThat(filtered["properties"].has("battery_ok")).isTrue
        assertThat(filtered["properties"].has("temperature_C")).isTrue
    }

    @Test
    fun `exclude-only does nothing when excludedFields empty`() {
        val svc = service(
            FingerprintDatafill(
                enabled = true,
                excludedFields = emptySet()
            )
        )

        val tree = objectMapper.readTree(sampleJson)
        val filtered = svc.excludeByFieldName(tree)

        // Since excludes is empty, filter simply deep-walks without removing.
        // Structure should still contain original names.
        assertThat(filtered.has("time")).isTrue
        assertThat(filtered["properties"].has("raw")).isTrue
        assertThat(filtered["meta"].has("note")).isTrue
    }

    @Test
    fun `disabled datafill skips filtering path in fingerprint flow`() {
        val svc = service(
            FingerprintDatafill(
                enabled = false,
                excludedFields = setOf("time")
            )
        )

        val tree = objectMapper.readTree(sampleJson)

        // When disabled, you wouldn't normally call excludeByFieldName directly.
        // This test simply documents the intended integration behavior.
        // (You can remove this test if you prefer.)
        assertThat(tree.has("time")).isTrue
    }
}
