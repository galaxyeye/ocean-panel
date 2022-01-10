package ai.platon.exotic

import ai.platon.exotic.crawl.MultiScraper
import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableJpaAuditing
class ExoticApplication(
    val applicationContext: ApplicationContext,
) {
    @Bean
    fun javaTimeModule(): JavaTimeModule {
        return JavaTimeModule()
    }

    @Bean
    fun scraper(): MultiScraper {
        return MultiScraper()
    }
}

fun main(args: Array<String>) {
    runApplication<ExoticApplication>(*args)
}
