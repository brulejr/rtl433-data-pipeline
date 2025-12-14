package io.jrb.labs.rtl433dp

import io.jrb.labs.commons.actuator.FeatureInfoContributor
import io.jrb.labs.commons.actuator.FeaturesInfoContributor
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.eventbus.SystemEventLogger
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class Rtl433DataPipelineApplication {

    @Bean
    fun systemEventBus(): SystemEventBus = SystemEventBus()

    @Bean
    fun systemEventLogger(systemEventBus: SystemEventBus): SystemEventLogger = SystemEventLogger(systemEventBus)

    @Bean
    fun featuresInfoContributor(contributors: List<FeatureInfoContributor>) = FeaturesInfoContributor(contributors)

}

fun main(args: Array<String>) {
	runApplication<Rtl433DataPipelineApplication>(*args)
}
