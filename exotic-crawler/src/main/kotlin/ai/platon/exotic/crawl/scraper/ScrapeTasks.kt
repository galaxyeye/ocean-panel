package ai.platon.exotic.crawl.scraper

import ai.platon.exotic.crawl.entity.PortalTask
import ai.platon.pulsar.driver.ScrapeResponse
import java.time.Duration
import java.time.Instant

class ListenablePortalTask(
    val task: PortalTask,
    val refresh: Boolean = false,
) {
    constructor(url: String) : this(PortalTask(url))

    var onSuccess: (ScrapeTask) -> Unit = { }
    var onFailure: (ScrapeTask) -> Unit = { }
    var onRetry: (ScrapeTask) -> Unit = { }

    var onItemSuccess: (ScrapeTask) -> Unit = { }
    var onItemFailure: (ScrapeTask) -> Unit = { }
    var onItemRetry: (ScrapeTask) -> Unit = { }
}

class ScrapeTask constructor(
    val url: String,
    val args: String,
    val priority: Int,
    val sqlTemplate: String,

    var id: String = "",

    var ruleId: Long? = null,
    var parentId: String = "",
    var parentUrl: String? = null,

    val createdTime: Instant = Instant.now(),
    var lastCheckTime: Instant = Instant.EPOCH,
    var timeout: Duration = Duration.ofHours(2),
    var exceptionMessage: String? = null,

    /**
     * Created, Submitted, Retry, Success, Timeout, Failed
     * */
    var status: String = "Created",
    var persisted: Boolean = false,
) {
    var response: ScrapeResponse = ScrapeResponse()

    /**
     * The companion portal task if it's a portal task
     * */
    var companionPortalTask: PortalTask? = null
    val isPortal get() = companionPortalTask != null

    val nextCheckTime get() = lastCheckTime.plusSeconds(response.estimatedWaitTime.coerceAtMost(60))
    val shouldCheck get() = nextCheckTime < Instant.now()

    val isTimeout get() = Duration.between(createdTime, Instant.now()) > timeout

    var exception: Exception? = null
}

class ListenableScrapeTask(
    val task: ScrapeTask
) {
    var onSuccess: (ListenableScrapeTask) -> Unit = { }
    var onFailure: (ListenableScrapeTask) -> Unit = { }
    var onRetry: (ListenableScrapeTask) -> Unit = { }
}
