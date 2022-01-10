package ai.platon.exotic.crawl.scraper

import ai.platon.pulsar.common.chrono.scheduleAtFixedRate
import ai.platon.pulsar.driver.Driver
import ai.platon.pulsar.driver.ScrapeException
import ai.platon.pulsar.driver.ScrapeResponse
import ai.platon.pulsar.driver.utils.SQLTemplate
import com.google.gson.Gson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap

open class TaskSubmitter(
    private val server: String,
    private val authToken: String,
    private val httpTimeout: Duration,
    private val autoCollect: Boolean = true,
) {
    var logger: Logger = LoggerFactory.getLogger(TaskSubmitter::class.java)
    val driver = Driver(server, authToken, httpTimeout)

    val pendingTasks: MutableMap<String, ListenableScrapeTask> = ConcurrentSkipListMap()
    val timeoutTasks: MutableMap<String, ListenableScrapeTask> = ConcurrentSkipListMap()
    val finishedTasks: MutableMap<String, ListenableScrapeTask> = ConcurrentSkipListMap()

    var taskCount = 0
        private set
    var finishedTaskCount = 0
        private set
    var successTaskCount = 0
        private set
    var failedTaskCount = 0
        private set
    var retryTaskCount = 0
        private set

    val pendingPortalTaskCount get() = pendingTasks.count { it.value.task.isPortal }
    val pendingTaskCount get() = pendingTasks.size

    private val collectTimer = Timer()

    init {
        if (autoCollect) {
            startCollectTimer()
        }
    }

    fun scrape(task: ListenableScrapeTask): ListenableScrapeTask {
        logger.info("Scraping 1/{} task | {} {}", pendingTasks.size, task.task.url, task.task.args)
        return submit(task)
    }

    fun scrapeAll(tasks: List<ListenableScrapeTask>): List<ListenableScrapeTask> {
        if (tasks.isEmpty()) {
            return listOf()
        }

        logger.info("Scraping {}/{} tasks", tasks.size, pendingTasks.size)

        submitAll(tasks)

        return tasks
    }

    private fun submit(listenableTask: ListenableScrapeTask): ListenableScrapeTask {
        ++taskCount

        val task = listenableTask.task
        try {
            val sql = SQLTemplate(task.sqlTemplate).createSQL(task.url + " " + task.args)
            val id = driver.submit(sql, task.priority, false)
            task.id = id
            pendingTasks[id] = listenableTask
        } catch (e: ScrapeException) {
            task.exception = e
            task.exceptionMessage = e.toString()
            logger.warn("Scrape failed", e)
        }

        return listenableTask
    }

    private fun submitAll(listenableTasks: List<ListenableScrapeTask>): List<ListenableScrapeTask> {
        return listenableTasks.map { submit(it) }
    }

    @Throws(InterruptedException::class)
    private fun collectTasks(): List<ScrapeResponse> {
        if (pendingTasks.isEmpty()) {
            return listOf()
        }

        val checkingTasks = pendingTasks.values
            .filter { it.task.shouldCheck }
            .sortedByDescending { it.task.lastCheckTime }
            .take(30)
        if (checkingTasks.isEmpty()) {
            val estimatedWaitTime = pendingTasks.values.minOfOrNull { it.task.response.estimatedWaitTime } ?: -1
            logger.info("No task to check, next task to wait: {}s", estimatedWaitTime)
            return listOf()
        }

        val checkingIds = checkingTasks.map { it.task.id }
        checkingTasks.forEach { it.task.lastCheckTime = Instant.now() }

        var fc = 0
        var rc = 0
        val startTime = Instant.now()
        val responses = driver.findAllByIds(checkingIds)
        responses.forEach { response ->
            val task = pendingTasks[response.id]
            if (task != null) {
                task.task.response = response
                when {
                    response.isDone -> {
                        ++fc
                        ++finishedTaskCount

                        if (response.statusCode == 200) {
                            task.onSuccess(task)
                        } else {
                            ++failedTaskCount
                            task.onFailure(task)
                        }
                    }
                    response.statusCode == 1601 -> {
                        ++rc
                        ++retryTaskCount
                        task.onRetry(task)
                    }
                }
            } else {
                response.resultSet = null
                logger.warn("Unexpected response {}\n{}", response.id, Gson().toJson(response))
            }
        }

        val roundFinishedTasks = checkingTasks.filter { it.task.response.isDone }
        roundFinishedTasks.forEach { pendingTasks.remove(it.task.id) }
        roundFinishedTasks.associateByTo(finishedTasks) { it.task.id }

        val timeoutTasks0 = pendingTasks.filter { it.value.task.isTimeout }
        if (timeoutTasks0.isNotEmpty()) {
            logger.info("Removing {} timeout tasks", timeoutTasks0.size)
            timeoutTasks0.forEach { pendingTasks.remove(it.key) }
            timeoutTasks.putAll(timeoutTasks0)
        }

        val estimatedWaitTime = responses.filter { !it.isDone }
            .minOfOrNull { it.estimatedWaitTime } ?: -1
        val elapsedTime = Duration.between(startTime, Instant.now())
        logger.info("Collected {}/{}/{}/{}/{} responses in {}, next task to wait: {}s",
            fc, rc, responses.size, checkingIds.size, pendingTasks.size, elapsedTime, estimatedWaitTime)

        return responses
    }

    private fun startCollectTimer() {
        val delay = Duration.ofSeconds(30)
        val period = Duration.ofSeconds(30)
        collectTimer.scheduleAtFixedRate(delay, period) { collectTasks() }
    }
}
