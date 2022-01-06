package ai.platon.exotic.crawl.entity

class ProductDetail(
    var uri: String,
    var baseUri: String,
    var price: Float,
    var priceText: String,
    var brand: String,
    var model: String
) {
    var properties: Map<String, Any?> = mutableMapOf()
    val isBrandMatched: Boolean
        get() = brand == (properties["brand"] ?: "").toString()
    val isModelMatched: Boolean
        get() = brand == (properties["model"] ?: "").toString()
    var allowDuplicate = false

    companion object {
        fun create(uri: String, properties: Map<String, Any?>, allowDuplicate: Boolean = false): ProductDetail {
            val product = ProductDetail(
                uri,
                (properties["base_uri"] ?: "").toString(), (properties["price"] ?: "0.0").toString().toFloat(),
                (properties["price_text"] ?: "").toString(),
                (properties["brand"] ?: "").toString(),
                (properties["model"] ?: "").toString()
            )
            product.properties = properties
            product.allowDuplicate = allowDuplicate
            return product
        }
    }
}
