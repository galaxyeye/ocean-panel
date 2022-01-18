package ai.platon.exotic.driver

import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.pulsar.driver.ScrapeResponse
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SubmitTests {
//    @MockK
//    lateinit var scrapeResponse: ScrapeResponse

    @Test
    fun testScrapeResponse() {
        val response = mockk<ScrapeResponse>()
        every { response.statusCode } returns 200

        println(response.statusCode)
        verify { response.statusCode }
        confirmVerified(response)
    }

    @Test
    fun testCrawlRuleRepository() {
        val rule = mockk<CrawlRule>()
        every { rule.id } returns 1

//        println(response.statusCode)
//        verify { response.statusCode }
//        confirmVerified(response)
    }
}
