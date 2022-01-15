package ai.platon.exotic.crawl

import ai.platon.exotic.common.authToken
import ai.platon.exotic.common.isDevelopment
import ai.platon.exotic.common.server
import ai.platon.exotic.crawl.entity.ItemPageModel
import ai.platon.exotic.crawl.scraper.ListenablePortalTask
import ai.platon.exotic.crawl.scraper.ListenableScrapeTask
import ai.platon.exotic.crawl.scraper.OutPageScraper
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

    val maxPendingTaskCount = if (isDevelopment) 10 else 50

    val pendingPortalTasks: Deque<ListenablePortalTask> = ConcurrentLinkedDeque()

    val pendingItems = ConcurrentLinkedQueue<ItemPageModel>()

    fun crawl() {
        val taskSubmitter = outPageScraper.taskSubmitter
        val submittedTaskCount = taskSubmitter.pendingTasks.size

        if (submittedTaskCount >= maxPendingTaskCount) {
            return
        }

        val n = (maxPendingTaskCount - submittedTaskCount).coerceAtMost(10)
        if (pendingPortalTasks.isEmpty()) {
            scrapeFromQueue(pendingPortalTasks, n)
            return
        }
    }

    @Throws(Exception::class)
    fun scrapeOutPages(task: ListenablePortalTask) {
        try {
            scrapeOutPages0(task)
        } catch (e: SQLException) {
            logger.warn(e.message)
        } catch (e: IOException) {
            logger.warn(e.message)
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

    @Throws(Exception::class)
    private fun scrapeOutPages0(portalTask: ListenablePortalTask) {
        val onItemSuccess: (ListenableScrapeTask) -> Unit = { task: ListenableScrapeTask ->
            val allowDuplicate = (task.task.ruleId ?: 0L) > 0L
            task.task.response.resultSet
                ?.filter { it.isNotEmpty() }
                ?.map { ItemPageModel.create(it["uri"].toString(), it, allowDuplicate) }
                ?.toCollection(pendingItems)
        }

        outPageScraper.scrape(portalTask, onItemSuccess)
    }
}

fun main() {
    val scraper = ExoticCrawler()
    scraper.crawl()

    readLine()
}
