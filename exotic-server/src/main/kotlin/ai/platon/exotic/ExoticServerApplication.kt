package ai.platon.exotic

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.collect.ExternalUrlLoader
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.CrawlLoops
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.protocol.browser.emulator.BrowserEmulatedFetcher
import ai.platon.pulsar.ql.h2.udfs.DomFunctionTables
import ai.platon.pulsar.ql.h2.udfs.DomFunctions
import ai.platon.pulsar.ql.h2.udfs.DomSelectFunctions
import ai.platon.pulsar.ql.h2.udfs.StringFunctions
import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import ai.platon.scent.boot.autoconfigure.component.ScentCrawlLoop
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.google.common.reflect.ClassPath
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportResource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.util.ClassUtils
import org.springframework.util.ResourceUtils
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader
import kotlin.reflect.KClass


@SpringBootApplication(
    scanBasePackages = [
        "ai.platon.scent.boot.autoconfigure",
        "ai.platon.scent.rest.api"
    ]
)
@ImportResource("classpath:config/app/app-beans/app-context.xml")
@EnableMongoRepositories("ai.platon.scent.boot.autoconfigure.persist")
class ExoticServerApplication(
    val globalCacheFactory: GlobalCacheFactory,
    val crawlLoop: ScentCrawlLoop,
    val crawlLoops: CrawlLoops,
    val urlLoader: ExternalUrlLoader,
    // active the bean
    val browserEmulatedFetcher: BrowserEmulatedFetcher,
    val unmodifiedConfig: ImmutableConfig,
    val applicationContext: ApplicationContext,
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
}

fun main(args: Array<String>) {
    SpringApplicationBuilder(ExoticServerApplication::class.java)
        .initializers(ScentContextInitializer())
        .registerShutdownHook(true)
        .run(*args)
}
