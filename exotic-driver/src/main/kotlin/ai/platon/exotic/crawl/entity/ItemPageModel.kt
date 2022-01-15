package ai.platon.exotic.crawl.entity

class ItemPageModel(
    var uri: String,
    var baseUri: String
) {
    var properties: Map<String, Any?> = mutableMapOf()
    var allowDuplicate = false

    companion object {
        fun create(uri: String, properties: Map<String, Any?>, allowDuplicate: Boolean = false): ItemPageModel {
            val product = ItemPageModel(uri, (properties["base_uri"] ?: "").toString())
            product.properties = properties
            product.allowDuplicate = allowDuplicate
            return product
        }
    }
}
