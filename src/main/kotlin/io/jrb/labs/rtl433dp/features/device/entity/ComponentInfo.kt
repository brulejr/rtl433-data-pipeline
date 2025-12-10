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

import com.fasterxml.jackson.annotation.JsonProperty

interface ComponentInfo {

    val platform: String

    val name: String?

    val availability: AvailabilityInfo?

    val availabilityMode: String?

    val availabilityTemplate: String?

    val availabilityTopic: String?

    val device: DeviceInfo?

    @get:JsonProperty("device_class")
    val deviceClass: String?

    @get:JsonProperty("enabled_by_default")
    val enabledByDefault: Boolean?

    val encoding: String?

    @get:JsonProperty("entity_category")
    val entityCategory: String?

    @get:JsonProperty("entity_picture")
    val entityPicture: String?

    @get:JsonProperty("expire_after")
    val expireAfter: Int

    @get:JsonProperty("force_update")
    val forceUpdate: Boolean

    val icon: String?

    @get:JsonProperty("json_attributes_template")
    val jsonAttributesTemplate: String?

    @get:JsonProperty("json_attributes_topic")
    val jsonAttributesTopic: String?

    @get:JsonProperty("object_id")
    val objectId: String?

    @get:JsonProperty("payload_available")
    val payloadAvailable: String?

    @get:JsonProperty("payload_not_available")
    val payloadNotAvailable: String?

    val qos: Int

    @get:JsonProperty("state_topic")
    val stateTopic: String?

    @get:JsonProperty("unique_id")
    val uniqueId: String?

    @get:JsonProperty("value_template")
    val valueTemplate: String?

}