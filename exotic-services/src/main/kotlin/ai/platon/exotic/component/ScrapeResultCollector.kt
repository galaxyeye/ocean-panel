package ai.platon.exotic.component

import ai.platon.exotic.common.FETCH_LIMIT
import ai.platon.exotic.common.PROP_FETCH_NEXT_OFFSET
import ai.platon.exotic.crawl.MultiScraper
import ai.platon.exotic.crawl.entity.ProductDetail
import ai.platon.exotic.entity.SysProp
import ai.platon.exotic.persistence.IntegratedProductRepository
import ai.platon.exotic.persistence.SysPropRepository
import ai.platon.exotic.persistence.converters.IntegratedProductConverter
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class ScrapeResultCollector(
    private val scraper: MultiScraper,
    private val sysPropRepository: SysPropRepository,
    private val integratedProductRepository: IntegratedProductRepository,
) {
    private val logger = LoggerFactory.getLogger(ScrapeResultCollector::class.java)

    fun synchronizeProducts() {
        var batchSize = 50
        val pendingProducts = scraper.pendingProducts
        val pendingProductCount = pendingProducts.size
        val productDetails = mutableListOf<ProductDetail>()
        while (batchSize-- > 0 && pendingProducts.isNotEmpty()) {
            val productDetail = scraper.pendingProducts.poll()
            if (productDetail != null) {
                productDetails.add(productDetail)
            }
        }

        val converter = IntegratedProductConverter()
        val unfilteredProducts = productDetails.map { converter.convert(it.properties) }
        val qualifiedResults = unfilteredProducts.filter { it.second.isQualified }
        val products = qualifiedResults.map { it.first }

        val stat = IntegratedProductConverter.globalStatistics
        logger.info("Synchronized {}/{} products, nn: {}/{}, np: {}/{}",
            products.size, pendingProductCount,
            converter.statistics.numNoName, stat.numNoName,
            converter.statistics.numNoPrice, stat.numNoPrice
        )
        integratedProductRepository.saveAll(products)
    }

    fun fetchAndSynchronizeProducts() {
        val prop = sysPropRepository.findByIdOrNull(PROP_FETCH_NEXT_OFFSET)
        var offset = prop?.value?.toLongOrNull() ?: 275406
        val limit = FETCH_LIMIT

        val driver = scraper.jdScraper.driver

        /**
         * TODO: properly handle unfinished tasks
         * */
        val result = driver.fetch(offset, limit)
        if (result.isEmpty()) {
            return
        }

        val converter = IntegratedProductConverter()
        val unfilteredProducts = result.mapNotNull { response ->
            response.resultSet?.map { converter.convert(it) }
        }.flatten()

        val qualifiedResults = unfilteredProducts.filter { it.second.isQualified }

        val products = qualifiedResults.map { it.first }
        integratedProductRepository.saveAll(products)

        val totalCount = driver.count()
        val stat = IntegratedProductConverter.globalStatistics
        logger.info("Synchronized {}/{} products from {}/{}, nn: {}/{}, np: {}/{}",
            products.size, result.size, offset, totalCount,
            converter.statistics.numNoName, stat.numNoName,
            converter.statistics.numNoPrice, stat.numNoPrice
        )

        offset += result.size
        sysPropRepository.save(SysProp(PROP_FETCH_NEXT_OFFSET, offset.toString()))
    }
}
