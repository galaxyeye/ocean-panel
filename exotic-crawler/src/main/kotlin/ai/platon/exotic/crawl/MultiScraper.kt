package ai.platon.exotic.crawl

import ai.platon.exotic.common.isDevelopment
import ai.platon.exotic.crawl.entity.ProductDetail
import ai.platon.exotic.crawl.scraper.ListenablePortalTask
import ai.platon.exotic.crawl.scraper.ListenableScrapeTask
import ai.platon.exotic.crawl.sites.JdScraper
import org.slf4j.LoggerFactory
import java.io.IOException
import java.sql.SQLException
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue

class MultiScraper {
    private val logger = LoggerFactory.getLogger(MultiScraper::class.java)
    private val server = "master"
    private val authToken = "b06plato27b102c8347a09f8acb160582ea97ea36"

    val jdScraper = JdScraper(server, authToken)

    val pendingPortalTasks: Deque<ListenablePortalTask> = ConcurrentLinkedDeque()

    val pendingProducts = ConcurrentLinkedQueue<ProductDetail>()

    fun crawl() {
        val scraper = jdScraper.outPageScraper.taskSubmitter
        val maxSubmittedTaskCount = if (isDevelopment) 2 else 50
        val submittedTaskCount = scraper.pendingTasks.size

        if (submittedTaskCount >= maxSubmittedTaskCount) {
            return
        }

        val n = (maxSubmittedTaskCount - submittedTaskCount).coerceAtMost(10)
        if (pendingPortalTasks.isEmpty()) {
            scrapeFromQueue(pendingPortalTasks, n)
            return
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

    @Throws(Exception::class)
    private fun scrapeOutPages0(portalTask: ListenablePortalTask) {
        val onItemSuccess: (ListenableScrapeTask) -> Unit = { task: ListenableScrapeTask ->
            val allowDuplicate = (task.task.ruleId ?: 0L) > 0L
            task.task.response.resultSet
                ?.filter { it.isNotEmpty() }
                ?.map { ProductDetail.create(it["uri"].toString(), it, allowDuplicate) }
                ?.toCollection(pendingProducts)
        }

        jdScraper.scrapeOutPages(portalTask, onItemSuccess)
    }
}

fun main() {
    val scraper = MultiScraper()
    scraper.crawl()

    readLine()
}
