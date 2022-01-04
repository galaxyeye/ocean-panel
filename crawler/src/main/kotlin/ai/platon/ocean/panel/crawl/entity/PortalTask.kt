package ai.platon.ocean.panel.crawl.entity

import ai.platon.ocean.panel.common.EPOCH_LDT
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.Instant
import java.time.LocalDateTime
import javax.persistence.*

/**
 * A portal task is a task start with a portal url
 * */
@Table(name = "portal_tasks")
@Entity
class PortalTask(
    var url: String,

    var args: String = "",

    var priority: Int = 0,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    var rule: CrawlRule? = null

    /**
     * The server side id
     * */
    var sid: String = ""

    var assignedCount: Int = 0

    var successCount: Int = 0

    var retryCount: Int = 0

    var failedCount: Int = 0

    var finishedCount: Int = 0

    var startTime: LocalDateTime = EPOCH_LDT

    /**
     * Created, Running, Finished, Timeout
     * */
    var status: String = "Created"

    @CreatedDate
    var createdDate: LocalDateTime = EPOCH_LDT

    @LastModifiedDate
    var lastModifiedDate: LocalDateTime = EPOCH_LDT
}
