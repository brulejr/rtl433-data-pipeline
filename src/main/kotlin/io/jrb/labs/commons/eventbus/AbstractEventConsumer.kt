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

package io.jrb.labs.commons.eventbus

import io.jrb.labs.commons.service.ControllableService
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

abstract class AbstractEventConsumer<G : Event, E : G> protected constructor (
    private val kClass: KClass<E>,
    private val eventBus: EventBus<G>,
    systemEventBus: SystemEventBus
) : ControllableService(systemEventBus) {

    protected val log = LoggerFactory.getLogger(javaClass)

    private var subscription: EventBus.Subscription? = null

    protected abstract suspend fun handleEvent(event: E)

    override fun onStart() {
        subscription = eventBus.subscribe(kClass.java) { event ->
            handleEvent(event)
        }
    }

    override fun onStop() {
        subscription?.cancel()
        subscription = null
    }

}
