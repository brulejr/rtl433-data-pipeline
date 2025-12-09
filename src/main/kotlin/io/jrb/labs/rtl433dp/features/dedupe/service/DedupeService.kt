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

package io.jrb.labs.rtl433dp.features.dedupe.service

import io.github.reactivecircus.cache4k.Cache
import io.jrb.labs.rtl433dp.events.PipelineEvent
import io.jrb.labs.rtl433dp.features.dedupe.DedupeDatafill
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

class DedupeService(datafill: DedupeDatafill) {

    private val log = LoggerFactory.getLogger(DedupeService::class.java)

    private val cache = Cache.Builder<String, String>()
        .expireAfterWrite(datafill.dedupeWindowInMilliseconds.milliseconds)
        .build()

    fun isUniqueEvent(event: PipelineEvent.Rtl433DataFingerprinted): Boolean {
        val deviceKey = event.deviceFingerprint
        val signature = event.eventFingerprint

        val previous = cache.get(deviceKey)
        val unique = if (previous != null && previous == signature) {
            false
        } else {
            cache.put(deviceKey, signature)
            true
        }

        log.info("Dedupe -> model = {}, id = {}, unique={}, deviceKey='{}', signature='{}'",
            event.data.model, event.data.id, unique, deviceKey, signature
        )

        return unique
    }

}