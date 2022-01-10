package ai.platon.exotic

import ai.platon.exotic.common.isDevelopment
import ai.platon.exotic.crawl.MultiScraper
import ai.platon.pulsar.common.DateTimes.MILLIS_OF_MINUTE
import ai.platon.pulsar.common.DateTimes.MILLIS_OF_SECOND
import ai.platon.pulsar.common.stringify
import ai.platon.exotic.component.CrawlTaskRunner
import ai.platon.exotic.component.ScrapeResultCollector
import ai.platon.exotic.persistence.PortalTaskRepository
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
        crawlTaskRunner.restartCrawlRulesNextRound()
    }

    @Scheduled(initialDelay = INITIAL_DELAY_2, fixedDelay = 10 * MILLIS_OF_SECOND)
    fun runPortalTasksWhenFew() {
        try {
            val submitter = scraper.jdScraper.outPageScraper.taskSubmitter
            val maxPendingTaskCount = if (isDevelopment) 2 else 50
            val pendingTaskCount = submitter.pendingTaskCount

            if (pendingTaskCount >= maxPendingTaskCount) {
                return
            }

            if (submitter.pendingPortalTaskCount > 2) {
                return
            }

            crawlTaskRunner.loadAndRunPortalTasks(2)
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

        portalTasks.forEach { it.status = "Finished" }
        portalTaskRepository.saveAll(portalTasks)

        portalTasks.forEach { submitter.finishedTasks.remove(it.sid) }
    }

    @Scheduled(initialDelay = INITIAL_DELAY_3, fixedDelay = 30 * MILLIS_OF_SECOND)
    fun synchronizeProducts() {
        crawlResultChecker.synchronizeProducts()
    }
}
