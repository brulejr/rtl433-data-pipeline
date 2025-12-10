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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BinarySensorInfo(
    override val platform: String = "binary_sensor",
    override val name: String = "MQTT Binary Sensor",
    override val availability: AvailabilityInfo? = null,
    override val availabilityMode: String? = null,
    override val availabilityTemplate: String? = null,
    override val availabilityTopic: String? = null,
    override val device: DeviceInfo? = null,
    override val deviceClass: String? = null,
    override val enabledByDefault: Boolean? = null,
    override val encoding: String? = null,
    override val entityCategory: String? = null,
    override val entityPicture: String? = null,
    override val expireAfter: Int = 0,
    override val forceUpdate: Boolean = false,
    override val icon: String? = null,
    override val jsonAttributesTemplate: String? = null,
    override val jsonAttributesTopic: String? = null,
    override val objectId: String? = null,
    @field:JsonProperty("off_delay") val offDelay: Int? = null,
    override val payloadAvailable: String? = null,
    override val payloadNotAvailable: String? = null,
    @field:JsonProperty("payload_off") val payloadOff: String? = null,
    @field:JsonProperty("payload_on") val payloadOn: String? = null,
    override val qos: Int = 0,
    override val stateTopic: String? = null,
    override val uniqueId: String? = null,
    override val valueTemplate: String? = null
) : ComponentInfo
