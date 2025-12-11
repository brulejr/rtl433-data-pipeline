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

package io.jrb.labs.rtl433dp.features.publishing.service

import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.service.ControllableService
import io.jrb.labs.rtl433dp.features.publishing.data.Target
import org.slf4j.LoggerFactory
import reactor.core.Disposable

class PublishingService(
    private val targets: List<Target>,
    systemEventBus: SystemEventBus
) : ControllableService(systemEventBus) {

    private val log = LoggerFactory.getLogger(PublishingService::class.java)

    private val _subscriptions: MutableMap<String, Disposable?> = mutableMapOf()

    override fun onStart() {
        targets.forEach { target ->
            log.info("connecting to target: {}", target.name)
            target.connect()
        }
    }

    override fun onStop() {
        targets.forEach { target ->
            log.info("disconnecting from target: {}", target.name)
            _subscriptions.remove(target.name)
            target.disconnect()
        }
    }

    fun publish(topic: String, message: String) {
        try {
            targets.forEach { target ->
                log.info("Publishing -> target = {}, topic = {}, message = {}", target.name, topic, message)
                target.publish(topic, message)
            }
        } catch(e: Exception) {
            log.error("Error: {}", e.message)
        }
    }

}