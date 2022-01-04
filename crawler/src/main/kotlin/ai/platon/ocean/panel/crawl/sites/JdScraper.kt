package ai.platon.ocean.panel.crawl.sites

import ai.platon.ocean.panel.crawl.scraper.ListenablePortalTask
import ai.platon.ocean.panel.crawl.scraper.ListenableScrapeTask
import ai.platon.ocean.panel.crawl.scraper.OutPageScraper
import ai.platon.pulsar.common.ResourceLoader

class JdScraper(
    private val server: String,
    private val authToken: String,
): SiteScraper {
    val level1SQLTemplate = ResourceLoader.readAllLines("sites/jd/template/extract/x-list.sql")
        .filterNot { it.startsWith("-- ") }
        .filterNot { it.isBlank() }
        .joinToString("\n")
    val level2SQLTemplate = ResourceLoader.readAllLines("sites/jd/template/extract/x-item.sql")
        .filter { line: String -> !line.startsWith("-- ") }
        .filter { line: String -> line.isNotBlank() }
        .joinToString("\n")

    val outPageScraper = OutPageScraper(server, authToken, level1SQLTemplate, level2SQLTemplate)

    override val driver get() = outPageScraper.taskSubmitter.driver

    override fun scrapeOutPages(
        listenablePortalTask: ListenablePortalTask,
        onItemSuccess: (ListenableScrapeTask) -> Unit,
        onItemFailure: (ListenableScrapeTask) -> Unit,
        onItemRetry: (ListenableScrapeTask) -> Unit,
    ) = outPageScraper.scrape(listenablePortalTask, onItemSuccess, onItemFailure, onItemRetry)
}
