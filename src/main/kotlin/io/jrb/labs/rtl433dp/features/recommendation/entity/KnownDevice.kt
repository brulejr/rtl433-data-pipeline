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

package io.jrb.labs.rtl433dp.features.recommendation.entity

import io.jrb.labs.rtl433dp.features.recommendation.resource.KnownDeviceResource
import io.jrb.labs.rtl433dp.features.recommendation.resource.PromotionRequest
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("known-devices")
data class KnownDevice(
    @Id val documentId: String? = null,
    val deviceId: String,
    val model: String,
    val fingerprint: String,
    val name: String,
    val type: String,
    val area: String,
    @CreatedDate val createdOn: Instant? = null,
    @LastModifiedDate val modifiedOn: Instant? = null,
    @Version val version: Int? = null
) {

    constructor(fingerprint: String, request: PromotionRequest) : this(
        model = request.model,
        deviceId = request.id,
        fingerprint = fingerprint,
        name = request.name,
        type = request.type,
        area = request.area
    )

    fun toKnownDeviceResource(): KnownDeviceResource {
        return KnownDeviceResource(
            model = this.model,
            deviceId = this.deviceId,
            fingerprint = this.fingerprint,
            name = this.name,
            type = this.type,
            area = this.area,
            createdOn = this.createdOn,
            modifiedOn = this.modifiedOn,
            version = this.version
        )
    }

}
