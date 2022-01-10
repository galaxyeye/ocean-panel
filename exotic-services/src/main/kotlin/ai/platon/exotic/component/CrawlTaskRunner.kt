package ai.platon.exotic.component

import ai.platon.exotic.common.isDevelopment
import ai.platon.exotic.crawl.MultiScraper
import ai.platon.exotic.crawl.entity.CrawlRule
import ai.platon.exotic.crawl.entity.PortalTask
import ai.platon.exotic.crawl.scraper.ListenablePortalTask
import ai.platon.exotic.persistence.CrawlRuleRepository
import ai.platon.exotic.persistence.PortalTaskRepository
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.common.urls.Urls
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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

    fun restartCrawlRulesNextRound() {
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
                logger.info("No portal urls in rule #{}", rule.id)
                return
            }

            val maxPages = if (isDevelopment) 2 else rule.maxPages
            val pagedPortalUrls = portalUrls.split("\n")
                .filter { Urls.isValidUrl(it) }
                .flatMap { url -> IntRange(1, maxPages).map { pg -> "$url&page=$pg" } }
            if (pagedPortalUrls.isEmpty()) {
                logger.info("No portal urls in rule #{}", rule.id)
            }

            val portalTasks = pagedPortalUrls.map {
                PortalTask(it, "-refresh", 3).also {
                    it.rule = rule
                    it.status = "Created"
                }
            }

            crawlRuleRepository.save(rule)
            portalTaskRepository.saveAll(portalTasks)

            logger.debug("Created {} portal tasks", portalTasks.size)
        } catch (t: Throwable) {
            logger.warn(t.stringify())
        }
    }

    fun loadAndRunPortalTasks(limit: Int) {
        val status = "Created"
        val order = Sort.Order.asc("id")
        val pageRequest = PageRequest.of(0, limit, Sort.by(order))
        val portalTasks = portalTaskRepository.findAllByStatus(status, pageRequest)
        if (portalTasks.isEmpty) {
            return
        }

        portalTasks.forEach {
            it.startTime = LocalDateTime.now()
            it.status = "Running"
        }
        portalTaskRepository.saveAll(portalTasks)

        portalTasks.map { ListenablePortalTask(it, true) }
            .shuffled()
            .forEach { task -> scraper.scrapeOutPages(task) }
    }
}
