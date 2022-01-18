package ai.platon.exotic.driver.crawl

import ai.platon.exotic.driver.common.authToken
import ai.platon.exotic.driver.common.isDevelopment
import ai.platon.exotic.driver.common.server
import ai.platon.exotic.driver.crawl.entity.ItemDetail
import ai.platon.exotic.driver.crawl.scraper.ListenablePortalTask
import ai.platon.exotic.driver.crawl.scraper.OutPageScraper
import ai.platon.exotic.driver.crawl.scraper.ScrapeTask
import org.slf4j.LoggerFactory
import java.io.IOException
import java.sql.SQLException
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue

class ExoticCrawler {
    private val logger = LoggerFactory.getLogger(ExoticCrawler::class.java)

    val outPageScraper = OutPageScraper(server, authToken)

    val driver get() = outPageScraper.taskSubmitter.driver

    val pendingPortalTasks: Deque<ListenablePortalTask> = ConcurrentLinkedDeque()

    val pendingItems = ConcurrentLinkedQueue<ItemDetail>()

    var maxPendingTaskCount = if (isDevelopment) 10 else 50

    fun crawl() {
        val taskSubmitter = outPageScraper.taskSubmitter
        val submittedTaskCount = taskSubmitter.pendingTaskCount

        if (submittedTaskCount >= maxPendingTaskCount) {
            return
        }

        val n = (maxPendingTaskCount - submittedTaskCount).coerceAtMost(10)
        if (pendingPortalTasks.isNotEmpty()) {
            scrapeFromQueue(pendingPortalTasks, n)
        }
    }

    @Throws(Exception::class)
    fun scrapeOutPages(task: ListenablePortalTask) {
        try {
//            task.onItemSuccess = {
//                createPendingItems(it)
//            }
            outPageScraper.scrape(task)
        } catch (t: Throwable) {
            logger.warn("Unexpected exception", t)
        }
    }

    private fun scrapeFromQueue(queue: Queue<ListenablePortalTask>, n: Int) {
        var n0 = n
        while (n0-- > 0) {
            val task = queue.poll()
            if (task != null) {
                scrapeOutPages(task)
            }
        }
    }

    private fun createPendingItems(task: ScrapeTask) {
        val allowDuplicate = task.companionPortalTask?.rule != null
        task.response.resultSet
            ?.filter { it.isNotEmpty() }
            ?.map { ItemDetail.create(it["uri"].toString(), it, allowDuplicate) }
            ?.toCollection(pendingItems)
    }
}

fun main() {
    val scraper = ExoticCrawler()
    scraper.crawl()

    readLine()
}
