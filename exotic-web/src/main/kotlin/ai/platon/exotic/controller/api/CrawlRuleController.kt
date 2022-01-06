package ai.platon.exotic.controller.api

import ai.platon.exotic.crawl.entity.CrawlRule
import ai.platon.exotic.persistence.CrawlRuleRepository
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/crawl/rules")
class CrawlRuleController(
    private val repository: CrawlRuleRepository
) {
    @GetMapping("/")
    fun list(): List<CrawlRule> {
        return repository.findAll()
    }

    @PostMapping("add")
    fun add(@RequestBody rule: CrawlRule): CrawlRule {
        rule.createdDate = LocalDateTime.now()
        rule.lastModifiedDate = rule.createdDate

        rule.adjustFields()
        repository.save(rule)

        return rule
    }
}
