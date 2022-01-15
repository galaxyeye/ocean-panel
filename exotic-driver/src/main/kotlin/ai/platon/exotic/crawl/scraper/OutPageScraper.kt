package ai.platon.exotic.crawl.scraper

import ai.platon.exotic.common.DEV_MAX_OUT_PAGES
import ai.platon.exotic.common.PRODUCT_MAX_OUT_PAGES
import ai.platon.pulsar.common.DateTimes
import ai.platon.exotic.common.isDevelopment
import ai.platon.exotic.crawl.entity.PortalTask
import ai.platon.pulsar.driver.ResourceStatus
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

open class OutPageScraper(
    server: String,
    authToken: String
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
        val task = listenablePortalTask.task
        val rule = task.rule
        if (rule == null) {
            logger.info("No rule for task {}", task.id)
            return
        }

        val taskTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
        val formattedTime = DateTimes.format(taskTime, "YYMMddHH")
        val taskIdSuffix = rule.id ?: formattedTime
        val taskId = "r$taskIdSuffix"
        var args = "-taskId $taskId -taskTime $taskTime"
        args += if (listenablePortalTask.refresh) " -refresh" else ""
        val priority = task.priority

        val outLinkSelector = rule.outLinkSelector
        if (outLinkSelector == null) {
            logger.info("No out link selector for task {}", task.id)
            return
        }

        val level1SQLTemplate = """
            select
                   dom_all_hrefs(dom, '$outLinkSelector') as hrefs
            from
                load_and_select('{{url}}', 'body');
        """.trimIndent()
        val scrapeTask = ScrapeTask(task.url, args, priority, level1SQLTemplate)
        scrapeTask.companionPortalTask = task

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
        val portalTask = task.task.companionPortalTask ?: return
        val rule = portalTask.rule ?: return

        val resultSet = task.task.response.resultSet
        if (resultSet == null || resultSet.isEmpty()) {
            logger.info("No result set | {} {}", task.task.url, task.task.args)
            return
        }

        var hrefs = resultSet[0]["hrefs"]?.toString()
        if (hrefs.isNullOrBlank()) {
            logger.info("No hrefs | {} {}", task.task.url, task.task.args)
            return
        }

        val maxOutPages = if (isDevelopment) DEV_MAX_OUT_PAGES else PRODUCT_MAX_OUT_PAGES
        hrefs = hrefs.removePrefix("(").removeSuffix(")")
        val urls = hrefs.split(",").take(maxOutPages)

        if (urls.isEmpty()) {
            logger.info("No url in portal task {} | {}", task.task.id, task.task.url)
            return
        }

        val taskTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
        val formattedTime = DateTimes.format(taskTime, "YYMMddHH")
        val taskIdSuffix = rule.id ?: formattedTime
        val taskId = "r$taskIdSuffix"
        var args = "-taskId $taskId -taskTime $taskTime -scrollCount 20"
        args += if (portalTask.args.contains("-refresh")) " -expires 2h" else " -expires 3600d"

        // child tasks
        val sqlTemplate = rule.sqlTemplate?.trim()
        if (sqlTemplate.isNullOrBlank()) {
            logger.warn("No sql template in rule {}", rule.id)
            return
        }

        val scrapeTasks = urls.map { url ->
            ScrapeTask(url, args, portalTask.priority, sqlTemplate).also {
                it.ruleId = rule.id ?: 0L
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
