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

package io.jrb.labs.rtl433dp.features.model.repository

import io.jrb.labs.rtl433dp.features.model.entities.ModelEntity
import io.jrb.labs.rtl433dp.features.model.resources.Rtl433Search
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Flux

class ModelRepositoryImpl(private val mongoTemplate: ReactiveMongoTemplate) : ModelRepositoryCustom {

    override fun search(search: Rtl433Search): Flux<ModelEntity> {
        val criteria = mutableListOf<Criteria>()

        search.model?.takeIf { it.isNotBlank() }?.let {
            criteria += Criteria.where("model").regex(".*${Regex.escape(it)}.*", "i")
        }
        search.id?.takeIf { it.isNotBlank() }?.let {
            criteria += Criteria.where("id").regex(".*${Regex.escape(it)}.*", "i")
        }
        search.name?.takeIf { it.isNotBlank() }?.let {
            criteria += Criteria.where("name").regex(".*${Regex.escape(it)}.*", "i")
        }
        search.type?.takeIf { it.isNotBlank() }?.let {
            criteria += Criteria.where("type").regex(".*${Regex.escape(it)}.*", "i")
        }
        search.area?.takeIf { it.isNotBlank() }?.let {
            criteria += Criteria.where("area").regex(".*${Regex.escape(it)}.*", "i")
        }

        val query = if (criteria.isEmpty()) Query()
        else Query(Criteria().andOperator(*criteria.toTypedArray()))

        return mongoTemplate.find(query, ModelEntity::class.java)
    }

}