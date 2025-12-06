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

package io.jrb.labs.rtl433dp.features.recommendation.api

import io.jrb.labs.commons.client.ResourceWrapper
import io.jrb.labs.commons.service.CrudResponse.Companion.crudResponse
import io.jrb.labs.rtl433dp.features.recommendation.resource.KnownDeviceResource
import io.jrb.labs.rtl433dp.features.recommendation.resource.PromotionRequest
import io.jrb.labs.rtl433dp.features.recommendation.service.KnownDeviceService
import io.jrb.labs.rtl433dp.features.recommendation.service.RecommendationService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/known-devices")
@ConditionalOnProperty(prefix = "application.recommendation", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class KnownDeviceController(
    private val knownDeviceService: KnownDeviceService,
    private val recommendationService: RecommendationService
) {

    @PostMapping("/promote/{fingerprint}")
    suspend fun promoteRecommendation(
        @PathVariable fingerprint: String,
        @RequestBody promotionRequest: PromotionRequest
    ): ResponseEntity<KnownDeviceResource> {
        // you’ll need a RecommendationRepository for this
        val recommendation = recommendationService.findByFingerprint(fingerprint).awaitSingleOrNull()
        if (recommendation != null) {
            val promoted = knownDeviceService.promoteRecommendation(fingerprint, promotionRequest)
            return ResponseEntity.ok(promoted)
        } else {
            return ResponseEntity.notFound().build()
        }
    }

    @GetMapping
    suspend fun listAll(): ResponseEntity<ResourceWrapper<List<KnownDeviceResource>>> {
        return crudResponse(
            actionFn = { knownDeviceService.retrieveKnownDeviceResources() }
        )
    }

}