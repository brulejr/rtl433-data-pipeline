package io.jrb.labs.rtl433dp

import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.eventbus.SystemEventLogger
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan( basePackages = ["io.jrb.labs.rtl433dp"])
class Rtl433DataPipelineApplication {

    @Bean
    fun systemEventBus(): SystemEventBus = SystemEventBus()

    @Bean
    fun systemEventLogger(systemEventBus: SystemEventBus): SystemEventLogger = SystemEventLogger(systemEventBus)

}

fun main(args: Array<String>) {
	runApplication<Rtl433DataPipelineApplication>(*args)
}
