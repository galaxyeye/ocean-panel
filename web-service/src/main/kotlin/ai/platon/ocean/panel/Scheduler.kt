package ai.platon.ocean.panel

import ai.platon.ocean.panel.component.CrawlTaskRunner
import ai.platon.ocean.panel.component.ScrapeResultCollector
import ai.platon.ocean.panel.crawl.MultiScraper
import ai.platon.ocean.panel.persistence.PortalTaskRepository
import ai.platon.pulsar.common.DateTimes.MILLIS_OF_MINUTE
import ai.platon.pulsar.common.DateTimes.MILLIS_OF_SECOND
import ai.platon.pulsar.common.stringify
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class Scheduler(
    private val scraper: MultiScraper,
    private val portalTaskRepository: PortalTaskRepository,
    private val crawlTaskRunner: CrawlTaskRunner,
    private val crawlResultChecker: ScrapeResultCollector,
) {
    companion object {
        const val INITIAL_DELAY = 30 * MILLIS_OF_SECOND
        const val INITIAL_DELAY_2 = 30 * MILLIS_OF_SECOND + 10 * MILLIS_OF_SECOND
        const val INITIAL_DELAY_3 = 30 * MILLIS_OF_SECOND + 20 * MILLIS_OF_SECOND
    }

    private val logger = LoggerFactory.getLogger(Scheduler::class.java)

    val submitter get() = scraper.jdScraper.outPageScraper.taskSubmitter

    @Bean
    fun runStartupTasks() {
        crawlTaskRunner.loadUnfinishedTasks()
    }

    @Scheduled(initialDelay = INITIAL_DELAY, fixedDelay = 10 * MILLIS_OF_SECOND)
    fun startCreatedCrawlRules() {
        crawlTaskRunner.startCreatedCrawlRules()
    }

    @Scheduled(initialDelay = INITIAL_DELAY, fixedDelay = 10 * MILLIS_OF_SECOND)
    fun restartCrawlRules() {
        crawlTaskRunner.restartCrawlRules()
    }

    @Scheduled(initialDelay = INITIAL_DELAY_2, fixedDelay = 10 * MILLIS_OF_SECOND)
    fun runResidentTasks() {
        try {
            scraper.crawl()
        } catch (t: Throwable) {
            logger.warn(t.stringify())
        }
    }

    @Scheduled(initialDelay = INITIAL_DELAY_3, fixedDelay = MILLIS_OF_MINUTE)
    fun updatePortalTaskStatus() {
        val portalTasks = submitter.finishedTasks.values.mapNotNull { it.task.companionPortalTask }
        if (portalTasks.isEmpty()) {
            return
        }

        portalTaskRepository.saveAll(portalTasks)
        portalTasks.forEach { submitter.finishedTasks.remove(it.sid) }
    }

    @Scheduled(initialDelay = INITIAL_DELAY_3, fixedDelay = 30 * MILLIS_OF_SECOND)
    fun synchronizeProducts() {
        // pending tasks not handled
        // crawlResultChecker.synchronizeProducts()
    }
}
