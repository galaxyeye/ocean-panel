package ai.platon.ocean.panel.persistence

import ai.platon.ocean.panel.crawl.entity.CrawlRule
import ai.platon.ocean.panel.crawl.entity.PortalTask
import ai.platon.ocean.panel.entity.SysProp
import ai.platon.ocean.panel.persistence.model.generated.FullFieldProduct
import ai.platon.ocean.panel.persistence.model.generated.IntegratedProduct
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

@Repository
interface FullFieldProductRepository : JpaRepository<FullFieldProduct, Serializable> {
    fun findAllByIdGreaterThan(id: Int): List<FullFieldProduct>
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
}

@Repository
interface SysPropRepository : JpaRepository<SysProp, Serializable> {
}
