package ai.platon.exotic

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.collect.ExternalUrlLoader
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.CrawlLoops
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.protocol.browser.emulator.BrowserEmulatedFetcher
import ai.platon.scent.BasicScentSession
import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import ai.platon.scent.boot.autoconfigure.component.ScentCrawlLoop
import ai.platon.scent.boot.autoconfigure.persist.CrawlSeedV3Repository
import ai.platon.scent.ql.h2.context.ScentSQLContexts
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportResource
import org.springframework.context.annotation.Scope
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@SpringBootApplication(
    scanBasePackages = [
        "ai.platon.scent.boot.autoconfigure",
        "ai.platon.scent.rest.api"
    ]
)
@ImportResource("classpath:config/app/app-beans/app-context.xml")
@EnableMongoRepositories("ai.platon.scent.boot.autoconfigure.persist")
class ExoticApplication(
    val globalCacheFactory: GlobalCacheFactory,
    val crawlLoop: ScentCrawlLoop,
    val crawlLoops: CrawlLoops,
    val urlLoader: ExternalUrlLoader,
    // active the bean
    val browserEmulatedFetcher: BrowserEmulatedFetcher,
    val unmodifiedConfig: ImmutableConfig,
    val applicationContext: ApplicationContext,
    val crawlSeedV3Repository: CrawlSeedV3Repository,
) {
    @Bean
    fun javaTimeModule(): JavaTimeModule {
        return JavaTimeModule()
    }

    @Bean
    fun commandLineRunner(ctx: ApplicationContext): CommandLineRunner {
        println("Context: $ctx")
        return CommandLineRunner { args ->
            val beans = ctx.beanDefinitionNames.sorted()
            val s = beans.joinToString("\n") { it }
            val path = AppPaths.getTmp("spring-beans.txt")
            AppFiles.saveTo(s, path)
        }
    }

    @Bean
    @Scope("prototype")
    fun getScentSession(): BasicScentSession {
        return ScentSQLContexts.activate(applicationContext).createSession()
    }

    @Bean
    fun initCrawlLoop() {
        crawlLoops.stop()
        crawlLoops.loops.clear()
        crawlLoops.loops.add(crawlLoop)
        crawlLoops.restart()
    }
}

fun main(args: Array<String>) {
    SpringApplicationBuilder(ExoticApplication::class.java)
        .profiles("rest", "master")
        .initializers(ScentContextInitializer())
        .registerShutdownHook(true)
        .run(*args)
}
