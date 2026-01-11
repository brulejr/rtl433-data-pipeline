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

    const val CONFIG_PREFIX_DEDUPE = "application.dedupe"
    const val CONFIG_PREFIX_DEVICE = "application.device"
    const val CONFIG_PREFIX_FINGERPRINT = "application.fingerprint"
    const val CONFIG_PREFIX_INGESTION = "application.ingestion"
    const val CONFIG_PREFIX_MODEL = "application.model"
    const val CONFIG_PREFIX_PUBLISHING = "application.publishing"
    const val CONFIG_PREFIX_RECOMMENDATION = "application.recommendation"
    const val CONFIG_PREFIX_SECURITY = "application.security"

    val DEDUPE = FeatureDescriptor(
        application = "rtl433dp",
        featureId = "dedupe",
        displayName = "Deduplication",
        description = "Suppresses duplicate RTL433 messages (e.g. burst repeats).",
        configPrefix = CONFIG_PREFIX_DEDUPE
    )

    val DEVICE = FeatureDescriptor(
        application = "rtl433dp",
        featureId = "device",
        displayName = "Device",
        description = "Manages RTL433 device information and metadata.",
        configPrefix = CONFIG_PREFIX_DEVICE
    )

    val FINGERPRINT = FeatureDescriptor(
        application = "rtl433dp",
        featureId = "fingerprint",
        displayName = "Fingerprint",
        description = "Generates structural fingerprints for incoming RTL433 data.",
        configPrefix = CONFIG_PREFIX_FINGERPRINT
    )

    val INGESTION = FeatureDescriptor(
        application = "rtl433dp",
        featureId = "ingestion",
        displayName = "Ingestion",
        description = "Ingests RTL433 data from source.",
        configPrefix = CONFIG_PREFIX_INGESTION
    )

    val MODEL = FeatureDescriptor(
        application = "rtl433dp",
        featureId = "model",
        displayName = "Model",
        description = "Extracts model information from RTL433 data.",
        configPrefix = CONFIG_PREFIX_MODEL
    )

    val PUBLISHING = FeatureDescriptor(
        application = "rtl433dp",
        featureId = "publishing",
        displayName = "Publishing",
        description = "Publishes RTL433 data to external systems.",
        configPrefix = CONFIG_PREFIX_PUBLISHING
    )

    val RECOMMENDATION = FeatureDescriptor(
        application = "rtl433dp",
        featureId = "recommendation",
        displayName = "Recommendation",
        description = "Provides recommendations based on RTL433 data analysis.",
        configPrefix = CONFIG_PREFIX_RECOMMENDATION
    )

    val ALL: List<FeatureDescriptor> = listOf(
        DEDUPE,
        DEVICE,
        FINGERPRINT,
        INGESTION,
        MODEL,
        PUBLISHING,
        RECOMMENDATION
    )

}
