package ai.platon.exotic.crawl.entity

class ProductOverview(var href: String, var price: Float, var priceText: String) {
    override fun toString(): String {
        return """{href: '$href', price: $price, priceText: '$priceText'}"""
    }

    companion object {
        fun create(fields: Map<String, Any>): ProductOverview {
            return ProductOverview(
                (fields["href"] ?: "").toString(), (fields["price"] ?: "0.0").toString().toFloat(),
                (fields["pricetext"] ?: "").toString()
            )
        }
    }
}
