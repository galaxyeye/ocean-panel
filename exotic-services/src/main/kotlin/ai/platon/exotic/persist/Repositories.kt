package ai.platon.exotic.persist

import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.exotic.driver.crawl.scraper.TaskStatus
import ai.platon.exotic.entity.SysProp
import ai.platon.exotic.persist.model.generated.FullFieldProduct
import ai.platon.exotic.persist.model.generated.IntegratedProduct
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

@Repository
interface CrawlRuleRepository : JpaRepository<CrawlRule, Serializable> {
}

@Repository
interface PortalTaskRepository : JpaRepository<PortalTask, Serializable> {
    fun findAllByStatusInAndCreatedDateGreaterThan(
        status: List<String>, createdDate: LocalDateTime
    ): List<PortalTask>

    fun findAllByStatus(status: TaskStatus, pageable: Pageable): Page<PortalTask>
}

@Repository
interface FullFieldProductRepository : JpaRepository<FullFieldProduct, Serializable> {
    fun findAllByIdGreaterThan(id: Long): List<FullFieldProduct>
}

@Repository
interface IntegratedProductRepository : JpaRepository<IntegratedProduct, Serializable> {
    fun findTopByOrderByIdDesc(): Optional<IntegratedProduct>
}

@Repository
interface SysPropRepository : JpaRepository<SysProp, Serializable>
