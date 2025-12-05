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
package io.jrb.labs.rtl433dp.types

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import java.time.Instant

@JsonPOJOBuilder(withPrefix = "")
class Rtl433DataBuilder {

    private var model: String? = null
    private var id: String? = null
    private var time: Instant = Instant.now()
    private var name: String? = null
    private var type: String? = null
    private var area: String? = null
    private val properties: MutableMap<String, Any?> = mutableMapOf()

    fun model(model: String) = apply { this.model = model }
    fun id(id: String) = apply { this.id = id }
    fun time(time: Instant) = apply { this.time = time }
    fun name(name: String?) = apply { this.name = name }
    fun type(type: String?) = apply { this.type = type }
    fun area(area: String?) = apply { this.area = area }

    @JsonAnySetter
    fun setProperty(key: String, value: Any?) = apply {
        properties[key] = value
    }

    fun build(): Rtl433Data {
        return Rtl433Data(
            model = requireNotNull(model),
            id = requireNotNull(id),
            time = time,
            properties = properties.toMap()
        )
    }

}