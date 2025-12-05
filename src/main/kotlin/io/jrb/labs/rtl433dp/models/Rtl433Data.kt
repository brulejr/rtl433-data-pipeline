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
package io.jrb.labs.rtl433dp.models

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.Instant

@JsonDeserialize(builder = Rtl433DataBuilder::class)
data class Rtl433Data(
    override val model: String,
    override val id: String,
    override val time: Instant,
    override val name: String? = null,
    override val type: String? = null,
    override val area: String? = null,
    private val properties: Map<String, Any?> = emptyMap(),
) : Device {

    @JsonAnyGetter
    fun getProperties(): Map<String, Any?> = properties

    operator fun contains(key: String): Boolean = properties.containsKey(key)
    operator fun get(key: String): Any? = properties[key]

}
