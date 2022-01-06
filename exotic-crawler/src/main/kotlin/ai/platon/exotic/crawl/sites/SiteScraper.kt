package ai.platon.exotic.crawl.sites

import ai.platon.exotic.crawl.scraper.ListenablePortalTask
import ai.platon.exotic.crawl.scraper.ListenableScrapeTask
import ai.platon.pulsar.driver.Driver

/**
 * The searcher interface
 */
interface SiteScraper {
    val driver: Driver

    fun scrapeOutPages(
        listenablePortalTask: ListenablePortalTask,
        onItemSuccess: (ListenableScrapeTask) -> Unit,
        onItemFailure: (ListenableScrapeTask) -> Unit = {},
        onItemRetry: (ListenableScrapeTask) -> Unit = {},
    )
}
