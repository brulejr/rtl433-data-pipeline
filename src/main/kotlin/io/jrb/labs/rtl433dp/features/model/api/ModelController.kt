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

package io.jrb.labs.rtl433dp.features.model.api

import com.fasterxml.jackson.annotation.JsonView
import io.jrb.labs.commons.client.ResourceViews
import io.jrb.labs.commons.client.ResourceWrapper
import io.jrb.labs.commons.service.CrudResponse.Companion.crudResponse
import io.jrb.labs.rtl433dp.features.model.service.ModelService
import io.jrb.labs.rtl433dp.features.model.resource.ModelResource
import io.jrb.labs.rtl433dp.features.model.resource.Rtl433Search
import io.jrb.labs.rtl433dp.features.model.resource.SensorsUpdateRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/models")
class ModelController(private val modelService: ModelService) {

    @GetMapping("/{modelName}/{fingerprint}")
    @JsonView(ResourceViews.Details::class)
    suspend fun getModel(
        @PathVariable modelName: String,
        @PathVariable fingerprint: String
    ): ResponseEntity<ResourceWrapper<ModelResource>> {
        return crudResponse(
            actionFn = { modelService.findModelResource(modelName, fingerprint) }
        )
    }

    @GetMapping
    @JsonView(ResourceViews.List::class)
    suspend fun retrieve(): ResponseEntity<ResourceWrapper<List<ModelResource>>> {
        return crudResponse(
            actionFn = { modelService.retrieveModelResources() }
        )
    }

    @PostMapping("/search")
    @JsonView(ResourceViews.List::class)
    suspend fun search(@RequestBody rtl433Search: Rtl433Search): ResponseEntity<ResourceWrapper<List<ModelResource>>> {
        return crudResponse(
            actionFn = { modelService.searchModelResources(rtl433Search) }
        )
    }

    @PostMapping("/{modelName}/{fingerprint}/sensors")
    suspend fun updateSensors(
        @PathVariable modelName: String,
        @PathVariable fingerprint: String,
        @RequestBody request: SensorsUpdateRequest
    ): ResponseEntity<ResourceWrapper<ModelResource>> {
        return crudResponse(
            actionFn = { modelService.updateSensors(modelName, fingerprint, request) }
        )
    }

}