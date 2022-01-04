package ai.platon.ocean.panel.crawl.sites

import ai.platon.ocean.panel.crawl.entity.ProductDetail
import ai.platon.ocean.panel.crawl.scraper.ListenablePortalTask
import ai.platon.ocean.panel.crawl.scraper.ListenableScrapeTask
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
