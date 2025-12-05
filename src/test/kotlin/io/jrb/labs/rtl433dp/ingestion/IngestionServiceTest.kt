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

// kotlin
package io.jrb.labs.rtl433dp.ingestion

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.commons.eventbus.SystemEvent
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.rtl433dp.events.PipelineEvent
import io.jrb.labs.rtl433dp.events.PipelineEventBus
import io.jrb.labs.rtl433dp.events.RawMessageSource
import io.jrb.labs.rtl433dp.features.ingestion.IngestionService
import io.jrb.labs.rtl433dp.features.ingestion.data.Source
import io.jrb.labs.rtl433dp.features.ingestion.data.SourceType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.Disposable

class IngestionServiceTest {

    // Simple in-test Source implementation (no mocking)
    private class TestSource(
        override val name: String,
        override val topic: String,
        override val type: SourceType
    ) : Source {
        var connected: Boolean = false
        var subscribed: Boolean = false
        private var handler: ((String) -> Unit)? = null

        override fun connect() {
            connected = true
        }

        override fun disconnect() {
            connected = false
        }

        override fun subscribe(topic: String, handler: (String) -> Unit): Disposable {
            subscribed = true
            this.handler = handler
            return object : Disposable {
                override fun dispose() {}
                override fun isDisposed(): Boolean = false
            }
        }

        fun emit(message: String) {
            handler?.invoke(message)
        }
    }

    @Test
    fun `start should publish system start, subscribe source and publish pipeline event when message received`() {
        runBlocking {
            val objectMapper = ObjectMapper()
            val pipelineBus = PipelineEventBus()
            val systemBus = SystemEventBus()

            val source = TestSource("RTL433", "topic-1", SourceType.MQTT)
            val service = IngestionService(listOf(source), pipelineBus, objectMapper, systemBus)

            // Listen for first pipeline event (single-shot) and accumulate system events
            val pipelineEventDeferred = CompletableDeferred<PipelineEvent>()
            val systemEvents = mutableListOf<SystemEvent>()

            val pipelineSub = pipelineBus.subscribe<PipelineEvent> { pipelineEventDeferred.complete(it) }
            val systemSub = systemBus.subscribe<SystemEvent> { systemEvents.add(it) }

            service.start()

            // wait for a system event published on start
            withTimeout(1_000) {
                while (systemEvents.isEmpty()) {
                    delay(10)
                }
            }

            assertThat(source.connected).isTrue()
            assertThat(source.subscribed).isTrue()

            // Emit a JSON payload that the default ObjectMapper can map (empty object often maps to a data class with optional fields).
            // If Rtl433Data requires fields, adapt the JSON accordingly in the test.
            source.emit("""
                {
                    "model": "test-model",
                    "id": "test-id"
                }
            """.trimIndent())

            // Wait for pipeline event
            val event = withTimeout(1_000) { pipelineEventDeferred.await() }
            assertThat(event).isInstanceOf(PipelineEvent.Rtl433DataReceived::class.java)
            val received = event as PipelineEvent.Rtl433DataReceived
            assertThat(received.source).isEqualTo(RawMessageSource.valueOf("RTL433"))
            assertThat(received.data).isNotNull()

            service.stop()

            // wait for a system event published on stop (total >= 2: start + stop)
            withTimeout(1_000) {
                while (systemEvents.size < 2) {
                    delay(10)
                }
            }

            assertThat(systemEvents).isNotEmpty()

            pipelineSub.cancel()
            systemSub.cancel()
        }
    }

    @Test
    fun `stop should disconnect sources`() {
        runBlocking {
            val objectMapper = ObjectMapper()
            val pipelineBus = PipelineEventBus()
            val systemBus = SystemEventBus()

            val source = TestSource("S2", "topic-2", SourceType.MQTT)
            val service = IngestionService(listOf(source), pipelineBus, objectMapper, systemBus)

            service.start()
            assertThat(source.connected).isTrue()

            service.stop()
            assertThat(source.connected).isFalse()
        }
    }

}
