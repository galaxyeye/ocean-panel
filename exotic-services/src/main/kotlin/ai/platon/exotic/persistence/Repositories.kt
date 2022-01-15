package ai.platon.exotic.persistence

import ai.platon.exotic.crawl.entity.CrawlRule
import ai.platon.exotic.crawl.entity.PortalTask
import ai.platon.exotic.entity.SysProp
import ai.platon.exotic.persistence.model.generated.FullFieldProduct
import ai.platon.exotic.persistence.model.generated.IntegratedProduct
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

@Repository
interface FullFieldProductRepository : JpaRepository<FullFieldProduct, Serializable> {
    fun findAllByIdGreaterThan(id: Long): List<FullFieldProduct>
}

@Repository
interface IntegratedProductRepository : JpaRepository<IntegratedProduct, Serializable> {
    fun findTopByOrderByIdDesc(): Optional<IntegratedProduct>
}

@Repository
interface CrawlRuleRepository : JpaRepository<CrawlRule, Serializable> {
}

@Repository
interface PortalTaskRepository : JpaRepository<PortalTask, Serializable> {
    fun findAllByStatusInAndCreatedDateGreaterThan(
        status: List<String>, createdDate: LocalDateTime
    ): List<PortalTask>

    fun findAllByStatus(status: String, pageable: Pageable): Page<PortalTask>
}

@Repository
interface SysPropRepository : JpaRepository<SysProp, Serializable>
