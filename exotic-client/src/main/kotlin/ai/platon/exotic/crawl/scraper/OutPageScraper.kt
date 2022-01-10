package ai.platon.exotic.crawl.scraper

import ai.platon.pulsar.common.DateTimes
import ai.platon.exotic.common.isDevelopment
import ai.platon.exotic.crawl.entity.ProductOverview
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

open class OutPageScraper(
    server: String,
    authToken: String,
    val level1SQLTemplate: String,
    val level2SQLTemplate: String,
) {
    var logger: Logger = LoggerFactory.getLogger(OutPageScraper::class.java)

    val httpTimeout: Duration = Duration.ofMinutes(3)

    val taskSubmitter: TaskSubmitter = TaskSubmitter(server, authToken, httpTimeout)

    @Throws(Exception::class)
    fun scrape(
        listenablePortalTask: ListenablePortalTask,
        onItemSuccess: (ListenableScrapeTask) -> Unit,
        onItemFailure: (ListenableScrapeTask) -> Unit = {},
        onItemRetry: (ListenableScrapeTask) -> Unit = {},
    ) {
        val taskTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
        val formattedTime = DateTimes.format(taskTime, "YYMMddHH")
        val taskIdSuffix = listenablePortalTask.task.rule?.id ?: formattedTime
        val taskId = "r$taskIdSuffix"
        var args = "-taskId $taskId -taskTime $taskTime"
        args += if (listenablePortalTask.refresh) " -refresh" else ""
        val priority = listenablePortalTask.task.priority

        val scrapeTask = ScrapeTask(listenablePortalTask.task.url, args, priority, level1SQLTemplate)
        scrapeTask.companionPortalTask = listenablePortalTask.task

        val listenableScrapeTask = ListenableScrapeTask(scrapeTask).also {
            it.onSuccess = { task: ListenableScrapeTask ->
                createChildTasks(task, onItemSuccess, onItemFailure, onItemRetry)
            }
            it.onRetry = { task: ListenableScrapeTask -> }
            it.onFailure = { task: ListenableScrapeTask -> }
        }

        taskSubmitter.scrape(listenableScrapeTask)
    }

    private fun createChildTasks(
        task: ListenableScrapeTask,
        onItemSuccess: (ListenableScrapeTask) -> Unit,
        onItemFailure: (ListenableScrapeTask) -> Unit = {},
        onItemRetry: (ListenableScrapeTask) -> Unit = {},
    ) {
        val resultSet = task.task.response.resultSet ?: return
        val productOverviews = resultSet.map { ProductOverview.create(it) }

        val maxOutPages = if (isDevelopment) 2 else 1000
        val productUrls = productOverviews
            .asSequence()
            .map { it.href }
            .filter { it.contains("item.jd.com") }
            .take(maxOutPages)
            .map { StringUtils.substringBefore(it, "?") }
            .toList()

        if (productUrls.isEmpty()) {
            logger.info("No product in task {} | {}", task.task.id, task.task.url)
            return
        }

        val portalTask = task.task.companionPortalTask ?: return
        val taskTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
        val formattedTime = DateTimes.format(taskTime, "YYMMddHH")
        val taskIdSuffix = portalTask.rule?.id ?: formattedTime
        val taskId = "r$taskIdSuffix"
        var args = "-taskId $taskId -taskTime $taskTime -scrollCount 20"
        args += if (portalTask.args.contains("-refresh")) " -expires 2h" else " -expires 3600d"

        // child tasks
        val scrapeTasks = productUrls.map { url ->
            ScrapeTask(url, args, portalTask.priority, level2SQLTemplate).also {
                it.ruleId = portalTask.rule?.id ?: 0L
                it.parentId = task.task.id
                it.parentUrl = task.task.url
            }
        }

        val listenableScrapeTasks = scrapeTasks.map { scrapeTask ->
            ListenableScrapeTask(scrapeTask).also {
                it.onSuccess = onItemSuccess
                it.onRetry = onItemRetry
                it.onFailure = onItemFailure
            }
        }

        taskSubmitter.scrapeAll(listenableScrapeTasks)
    }
}
