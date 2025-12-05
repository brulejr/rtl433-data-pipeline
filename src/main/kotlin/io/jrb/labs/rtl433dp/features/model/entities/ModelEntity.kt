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

package io.jrb.labs.rtl433dp.features.model.entities

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.commons.model.Entity
import io.jrb.labs.rtl433dp.features.model.resources.ModelResource
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import kotlin.collections.map
import kotlin.let

@Document("models")
data class ModelEntity(
    override val id: String? = null,
    override val guid: String? = null,
    override val ownerGuid: String? = null,
    val source: String,
    val model: String,
    val jsonStructure: String,
    val fingerprint: String,
    val category: String? = null,
    val sensors: List<SensorMapping>? = null,
    override val createdOn: Instant? = null,
    override val createdBy: String? = null,
    override val modifiedOn: Instant? = null,
    override val modifiedBy: String? = null,
    override val version: Long? = null
) : Entity<ModelEntity> {

    override fun withCreateInfo(guid: String?, userGuid: String?): ModelEntity {
        return copy(
            guid = guid,
            ownerGuid = userGuid,
            createdBy = userGuid,
            createdOn = Instant.now(),
            version = 1
        )
    }

    override fun withUpdateInfo(userGuid: String?): ModelEntity {
        return copy(
            modifiedBy = userGuid,
            modifiedOn = Instant.now(),
            version = version?.plus(1)
        )
    }

    fun toModelResource(objectMapper: ObjectMapper): ModelResource {
        return ModelResource(
            source = this.source,
            model = this.model,
            jsonStructure = this.jsonStructure.let { objectMapper.readTree(it) },
            fingerprint = this.fingerprint,
            category = category,
            sensors = this.sensors?.map { it.toSensorMappingResource() },
            createdOn = this.createdOn,
            modifiedOn = this.modifiedOn,
            version = this.version
        )
    }

}
