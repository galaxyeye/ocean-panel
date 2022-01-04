package ai.platon.ocean.panel

import ai.platon.ocean.panel.crawl.MultiScraper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class OceanApplication {
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
    runApplication<OceanApplication>(*args)
}
