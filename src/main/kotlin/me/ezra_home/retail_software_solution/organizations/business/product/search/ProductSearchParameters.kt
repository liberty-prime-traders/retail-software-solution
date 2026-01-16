package me.ezra_home.retail_software_solution.organizations.business.product.search

import me.ezra_home.retail_software_solution.util.enums.ProductStatus
import java.util.UUID

enum class SearchMode {
    NONE,       // No text search
    FULLTEXT,   // tsvector @@ tsquery
    TRIGRAM,    // trigram % similarity
    PREFIX,     // ILIKE 'text%'
    WILDCARD    // ILIKE '%text%'
}

data class ProductSearchParameters(
    val productNameOrDescription: String? = null,
    val referenceNumber: String? = null,
    val categoryIds: Set<UUID> = emptySet(),
    val productGroupIds: Set<UUID> = emptySet(),
    val tagsIds: Set<UUID> = emptySet(),
    val statusList: Set<ProductStatus> = setOf(ProductStatus.ACTIVE),
    val searchMode: SearchMode = SearchMode.NONE
) {
    init {
        require(statusList.isNotEmpty()) {
            "statusList cannot be empty. It must contain at least one status value."
        }
    }
}
