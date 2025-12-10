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

package io.jrb.labs.rtl433dp.features.device.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.service.ControllableService
import io.jrb.labs.commons.service.CrudOutcome
import io.jrb.labs.rtl433dp.events.PipelineEvent
import io.jrb.labs.rtl433dp.features.device.DeviceDatafill
import io.jrb.labs.rtl433dp.features.device.entity.BinarySensorInfo
import io.jrb.labs.rtl433dp.features.device.entity.ComponentInfo
import io.jrb.labs.rtl433dp.features.device.entity.DeviceInfo
import io.jrb.labs.rtl433dp.features.device.entity.DeviceInfo.Companion.deviceInfo
import io.jrb.labs.rtl433dp.features.device.entity.DiscoveryInfo
import io.jrb.labs.rtl433dp.features.device.entity.OriginInfo.Companion.originInfo
import io.jrb.labs.rtl433dp.features.device.entity.SensorInfo
import io.jrb.labs.rtl433dp.features.model.service.ModelService
import io.jrb.labs.rtl433dp.resources.SensorMappingResource
import io.jrb.labs.rtl433dp.types.Rtl433Data
import io.jrb.labs.rtl433dp.types.SensorType
import org.slf4j.LoggerFactory
import java.time.Instant

class DeviceService(
    private val datafill: DeviceDatafill,
    private val objectMapper: ObjectMapper,
    private val modelService: ModelService,
    systemEventBus: SystemEventBus
) : ControllableService(systemEventBus) {

    private val log = LoggerFactory.getLogger(DeviceService::class.java)

    suspend fun processEvent(event: PipelineEvent.KnownDevice): Map<String, String> {
        val modelName = event.data.model
        val modelFingerprint = event.modelFingerprint
        when (val model = modelService.findModelResource(modelName, modelFingerprint)) {
            is CrudOutcome.Success -> {
                val sensors = model.data.sensors ?: emptyList()
                val deviceDiscovery = toHomeAssistantDeviceDiscovery(event.data, sensors)
                val deviceData = toHomeAssistantSensor(event.data, sensors)
                log.info("Device -> deviceData = {}, deviceDiscovery = {}", deviceData, deviceDiscovery)
                return mapOf(deviceDiscovery, deviceData)
            }
            else -> {
                log.error("Failed to find model '{}' for known device fingerprint: {}", modelName, event.deviceFingerprint)
                return emptyMap()
            }
        }
    }

    private fun toHomeAssistantDeviceDiscovery(rtl433Data: Rtl433Data, sensors: List<SensorMappingResource>): Pair<String, String> {
        val device = deviceInfo(rtl433Data)
        val origin = originInfo()
        val components = rtl433Data.getProperties()
            .filter { (key, _) -> isSensor(key, sensors) }
            .map { (key, _) ->
                componentInfo(key, device, rtl433Data, sensors)
            }.toMap()
        val discoveryInfo = DiscoveryInfo(
            device = device,
            origin = origin,
            stateTopic = stateTopic(device),
            components = components
        )
        return configTopic(device) to objectMapper.writeValueAsString(discoveryInfo)
    }

    private fun toHomeAssistantSensor(rtl433Data: Rtl433Data, sensors: List<SensorMappingResource>): Pair<String, String> {
        val device = deviceInfo(rtl433Data)
        val data = buildMap {
            put("timestamp", Instant.now())
            put("device", device.name)
            putAll(normalizeData(rtl433Data.getProperties(), sensors))
        }
        return stateTopic(device) to objectMapper.writeValueAsString(data)
    }


    private fun configTopic(device: DeviceInfo): String {
        return datafill.configTopic.format(device.name)
    }

    private fun stateTopic(device: DeviceInfo): String {
        return datafill.stateTopic.format(device.name)
    }

    private fun findSensor(key: String, sensors: List<SensorMappingResource>): SensorMappingResource {
        return sensors.find { it.name == key } ?: throw IllegalStateException("$key does not exist")
    }

    private fun isSensor(key: String, sensors: List<SensorMappingResource>): Boolean {
        return sensors.any { it.name == key }
    }

    private fun normalizeData(data: Map<String, Any?>, sensors: List<SensorMappingResource>): Map<String, Any?> {
        return data.filter { (key, _) -> isSensor(key, sensors) }.mapValues { (key, value) ->
            when (findSensor(key, sensors).type) {
                SensorType.ANALOG -> value
                SensorType.BINARY -> when (key) {
                    "battery_ok" ->  if (value == 1) "OFF" else "ON"
                    "closed" ->  if (value == 1) "OFF" else "ON"
                    else -> if (value == 0) "OFF" else "ON"
                }
            }
        }
    }

    private fun inferUnit(key: String): String? {
        return when {
            key.contains("temperature", ignoreCase = true) -> "°C"
            key.contains("humidity", ignoreCase = true) -> "%"
            key.contains("battery", ignoreCase = true) -> "%"
            key.contains("pressure", ignoreCase = true) -> "hPa"
            else -> null
        }
    }

    private fun componentInfo(
        key: String,
        device: DeviceInfo,
        rtl433Data: Rtl433Data,
        sensors: List<SensorMappingResource>
    ): Pair<String, ComponentInfo> {
        val uniqueId = "${rtl433Data.model}_${rtl433Data.id}_$key"
        val sensorDefinition = findSensor(key, sensors)
        val sensorName = sensorDefinition.name
        val sensor = when (sensorDefinition.type) {
            SensorType.ANALOG -> SensorInfo(
                deviceClass = sensorDefinition.classname,
                name = "${device.name}_$sensorName",
                uniqueId = uniqueId,
                unitOfMeasurement = inferUnit(key),
                valueTemplate = "{{ value_json.$key }}"
            )
            SensorType.BINARY -> BinarySensorInfo(
                deviceClass = sensorDefinition.classname,
                name = "${device.name}_$key",
                uniqueId = uniqueId,
                valueTemplate = "{{ value_json.$key }}"
            )
        }
        return uniqueId to sensor
    }

}