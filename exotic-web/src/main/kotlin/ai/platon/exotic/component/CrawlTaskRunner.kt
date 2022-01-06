package ai.platon.exotic.component

import ai.platon.exotic.common.isDevelopment
import ai.platon.exotic.crawl.MultiScraper
import ai.platon.exotic.crawl.entity.CrawlRule
import ai.platon.exotic.crawl.entity.PortalTask
import ai.platon.exotic.crawl.scraper.ListenablePortalTask
import ai.platon.exotic.persistence.CrawlRuleRepository
import ai.platon.exotic.persistence.PortalTaskRepository
import ai.platon.pulsar.common.stringify
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Component
class CrawlTaskRunner(
    val crawlRuleRepository: CrawlRuleRepository,
    val portalTaskRepository: PortalTaskRepository,
    val scraper: MultiScraper
) {
    private val logger = LoggerFactory.getLogger(CrawlTaskRunner::class.java)

    fun loadUnfinishedTasks() {
//        val tasks = pendingPortalTaskRepository.findAll()
//        pendingPortalTaskRepository.deleteAll()
//        val portalTasks = tasks
//            .map { Gson().fromJson(it.data, PortalTask::class.java) }
//            .map { ListenablePortalTask(it, true) }
//
//        logger.info("Loaded {} unfinished tasks", tasks.size)
//        portalTasks.forEach { task -> scraper.scrapeOutPages(task) }
    }

    fun startCreatedCrawlRules() {
        val now = LocalDateTime.now()
        val rules = crawlRuleRepository.findAll()
            .filter { it.status == "Created" }
            .filter { it.startTime <= now }

        rules.forEach { rule -> startCrawl(rule) }
    }

    fun restartCrawlRules() {
        val now = LocalDateTime.now()
        val rules = crawlRuleRepository.findAll()
            .filter { it.status == "Running" || it.status == "Finished" }
            .filter { it.lastCrawlTime + it.period <= now }

        rules.forEach { rule -> startCrawl(rule) }
    }

    fun startCrawl(rule: CrawlRule) {
        try {
            rule.status = "Running"
            rule.lastCrawlTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
            rule.lastModifiedDate = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)

            val portalUrls = rule.portalUrls

            if (portalUrls.isBlank()) {
                rule.status = "Finished"
                return
            }

            val maxPages = if (isDevelopment) 2 else rule.maxPages
            val pagedPortalUrls = portalUrls.split("\n")
                .flatMap { url -> IntRange(1, maxPages).map { pg -> "$url&page=$pg" } }

            val portalTasks = pagedPortalUrls.map {
                PortalTask(it, "-refresh", 3).also {
                    it.rule = rule
                    it.status = "Running"
                }
            }

            crawlRuleRepository.save(rule)

            portalTasks.forEach { portalTaskRepository.save(it) }

            portalTasks.map { ListenablePortalTask(it, true) }
                .shuffled()
                .forEach { task -> scraper.pendingPortalTasks.addFirst(task) }
        } catch (t: Throwable) {
            logger.warn(t.stringify())
        }
    }
}
