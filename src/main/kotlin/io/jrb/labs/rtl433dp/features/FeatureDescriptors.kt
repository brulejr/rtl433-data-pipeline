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

package io.jrb.labs.rtl433dp.features

import io.jrb.labs.commons.feature.FeatureDescriptor

object FeatureDescriptors {

    val DEDUPE = FeatureDescriptor(
        application = "rtl433dp",
        featureId = "dedupe",
        displayName = "Deduplication",
        description = "Suppresses duplicate RTL433 messages (e.g. burst repeats).",
        configPrefix = "rtl433dp.features.dedupe"
    )

    val FINGERPRINT = FeatureDescriptor(
        application = "rtl433dp",
        featureId = "fingerprint",
        displayName = "Fingerprint",
        description = "Generates structural fingerprints for incoming RTL433 data.",
        configPrefix = "rtl433dp.features.fingerprint"
    )

    val INGESTION = FeatureDescriptor(
        application = "rtl433dp",
        featureId = "ingestion",
        displayName = "Ingestion",
        description = "Ingests RTL433 data from source.",
        configPrefix = "rtl433dp.features.ingestion"
    )

    val ALL: List<FeatureDescriptor> = listOf(
        DEDUPE,
        FINGERPRINT,
        INGESTION
    )

}
