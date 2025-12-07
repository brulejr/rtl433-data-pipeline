package io.jrb.labs.rtl433dp.events

import io.jrb.labs.commons.eventbus.Event
import io.jrb.labs.rtl433dp.types.Rtl433Data

sealed class PipelineEvent : Event {

    data class Rtl433DataReceived(
        val source: RawMessageSource,
        val data: Rtl433Data
    ) : PipelineEvent()

    data class Rtl433DataFingerprinted(
        val source: RawMessageSource,
        val data: Rtl433Data,
        val deviceFingerprint: String,
        val modelFingerprint: String,
        val modelStructure: String
    ) : PipelineEvent()

}