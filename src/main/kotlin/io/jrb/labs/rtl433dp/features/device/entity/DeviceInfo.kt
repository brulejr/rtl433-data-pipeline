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

package io.jrb.labs.rtl433dp.features.device.entity

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.jrb.labs.rtl433dp.types.Rtl433Data

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DeviceInfo(

    @field:JsonProperty("configuration_url")
    val configurationUrl: String? = null,

    val connections: List<Pair<String, String>>? = null,

    @field:JsonProperty("hw_version")
    val hwVersion: String? = null,

    val identifiers: List<String>? = null,

    val manufacturer: String? = null,

    val model: String? = null,

    @field:JsonProperty("model_id")
    val modelId: String? = null,

    val name: String? = null,

    @field:JsonProperty("serial_number")
    val serialNumber: String? = null,

    @field:JsonProperty("suggested_area")
    val suggestedArea: String? = null,

    @field:JsonProperty("sw_version")
    val swVersion: String? = null,

    @field:JsonProperty("via_device")
    val viaDevice: String? = null

) {
    companion object {
        fun deviceInfo(rtl433Data: Rtl433Data): DeviceInfo {
            return DeviceInfo(
                identifiers = listOf(rtl433Data.id),
                name = "rtl433-${rtl433Data.name}",
                manufacturer = rtl433Data.model,
                model = rtl433Data.type,
                serialNumber = rtl433Data.id,
                suggestedArea = rtl433Data.area
            )
        }
    }
}
