package io.jrb.labs.rtl433dp.features.ingestion.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.metrics.FeatureMetrics
import io.jrb.labs.commons.service.ControllableService
import io.jrb.labs.rtl433dp.events.PipelineEvent
import io.jrb.labs.rtl433dp.events.PipelineEventBus
import io.jrb.labs.rtl433dp.events.RawMessageSource
import io.jrb.labs.rtl433dp.features.ingestion.data.Source
import io.jrb.labs.rtl433dp.features.ingestion.data.SourceType
import io.jrb.labs.rtl433dp.types.Rtl433Data
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import reactor.core.Disposable
import java.util.concurrent.atomic.AtomicReference

class IngestionService(
    private val sources: List<Source>,
    private val featureMetrics: FeatureMetrics,
    private val objectMapper: ObjectMapper,
    private val eventBus: PipelineEventBus,
    systemEventBus: SystemEventBus
) : ControllableService(systemEventBus) {

    init {
        featureMetrics.featureStateGauge(this::isRunning)
    }

    private val log = LoggerFactory.getLogger(IngestionService::class.java)

    private val subscriptions: MutableMap<String, Disposable?> = mutableMapOf()

    private val receivedCounter = featureMetrics.eventCounter("received")
    private val errorCounter = featureMetrics.errorCounter("ingestion")

    private val scopeRef = AtomicReference<CoroutineScope?>(null)

    private fun newScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onStart() {
        scopeRef.getAndSet(newScope())?.cancel()
        val scope = scopeRef.get()!!

        sources.forEach { source ->
            log.info("starting source subscription: {}", source.name)

            // ✅ Compatibility + avoids MQTT double-connect:
            // - For MQTT sources, connect/reconnect happens inside subscribe() (HiveMqttSource).
            // - For non-MQTT sources, we keep the historical eager connect behavior.
            if (source.type != SourceType.MQTT) {
                try {
                    source.connect()
                } catch (e: Exception) {
                    log.warn("Failed to connect source {}", source.name, e)
                }
            }

            subscriptions[source.name]?.dispose()
            subscriptions[source.name] = source.subscribe(source.topic) { message ->
                scope.launch {
                    featureMetrics.processingTimer(source.type.toString()) {
                        try {
                            val rtl433Data = objectMapper.readValue(message, Rtl433Data::class.java)
                            log.debug(
                                "Data -> model = {}, id = {}, rtl433Data='{}'",
                                rtl433Data.model, rtl433Data.id, rtl433Data
                            )
                            eventBus.send(
                                PipelineEvent.Rtl433DataReceived(
                                    source = RawMessageSource.valueOf(source.name),
                                    data = rtl433Data
                                )
                            )
                        } catch (e: Exception) {
                            errorCounter.increment()
                            log.error("Error while processing message for ingestion {}", message, e)
                        } finally {
                            receivedCounter.increment()
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        // Cancel first so we don't schedule more work while shutting down
        scopeRef.getAndSet(null)?.cancel()

        // Dispose subscriptions
        sources.forEach { source ->
            subscriptions.remove(source.name)?.dispose()
        }

        // Disconnect sources
        sources.forEach { source ->
            log.info("stopping source subscription: {}", source.name)
            try {
                source.disconnect()
            } catch (e: Exception) {
                log.warn("Failed to disconnect source {}", source.name, e)
            }
        }
    }
}