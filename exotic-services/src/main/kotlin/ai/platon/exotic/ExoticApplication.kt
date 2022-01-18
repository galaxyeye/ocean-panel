package ai.platon.exotic

import ai.platon.exotic.driver.crawl.ExoticCrawler
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

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
    fun exoticCrawler(): ExoticCrawler {
        return ExoticCrawler()
    }
}

fun main(args: Array<String>) {
    runApplication<ExoticApplication>(*args)
}
