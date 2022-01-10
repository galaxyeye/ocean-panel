package ai.platon.exotic.crawl.entity

import ai.platon.exotic.common.EPOCH_LDT
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.persistence.*

@Table(name = "crawl_rules")
@Entity
@EntityListeners(AuditingEntityListener::class)
class CrawlRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @Column(name = "name")
    var name: String = randomName()

    @Column(name = "label")
    var label: String? = null

    @Lob
    @Column(name = "portal_urls")
    var portalUrls: String = ""

    @Column(name = "description")
    var description: String? = null

    @Column(name = "max_pages")
    var maxPages: Int = 30

    @Column(name = "start_time")
    var startTime: LocalDateTime = EPOCH_LDT
        .truncatedTo(ChronoUnit.SECONDS)

    @Column(name = "dead_time")
    var deadTime: LocalDateTime = LocalDateTime.parse("2200-01-01T08:00")

    @Column(name = "last_crawl_time")
    var lastCrawlTime: LocalDateTime = EPOCH_LDT
        .truncatedTo(ChronoUnit.SECONDS)

    @Column(name = "period")
    var period: Duration = Duration.ofDays(3650)

    /**
     * Enum: Created, Running, Paused
     * */
    @Column(name = "status")
    var status: String = "Created"

    @CreatedDate
    @Column(name = "created_date")
    var createdDate: LocalDateTime = EPOCH_LDT

    @LastModifiedDate
    @Column(name = "last_modified_date")
    var lastModifiedDate: LocalDateTime = EPOCH_LDT

    @OneToMany(fetch = FetchType.LAZY)
    val portalTasks: MutableList<PortalTask> = mutableListOf()

    final fun randomName(): String {
        return "T" + RandomStringUtils.randomAlphanumeric(6).lowercase()
    }

    final fun adjustFields() {
        period = period.truncatedTo(ChronoUnit.MINUTES)
        startTime = startTime.truncatedTo(ChronoUnit.SECONDS)
        lastCrawlTime = lastCrawlTime.truncatedTo(ChronoUnit.SECONDS)
        createdDate = createdDate.truncatedTo(ChronoUnit.SECONDS)
        lastModifiedDate = lastModifiedDate.truncatedTo(ChronoUnit.SECONDS)

        if (startTime == EPOCH_LDT) {
            startTime = lastCrawlTime
        }

        name = name.takeIf { it.isNotBlank() } ?: randomName()
        label = label ?: ""
        maxPages = maxPages ?: 30
    }
}
