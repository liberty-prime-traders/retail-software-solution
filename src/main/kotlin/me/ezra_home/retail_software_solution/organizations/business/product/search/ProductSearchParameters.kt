package me.ezra_home.retail_software_solution.organizations.business.product.search

import me.ezra_home.retail_software_solution.util.enums.ProductStatus
import java.util.UUID

enum class SearchStrategy {
    NONE,
    FULLTEXT,
    TRIGRAM,
    PREFIX,
    WILDCARD
}

data class ProductSearchParameters(
    val searchText: String? = null,
    val referenceNumber: String? = null,
    val categoryIds: Set<UUID> = emptySet(),
    val tagsIds: Set<UUID> = emptySet(),
    val statusList: Set<ProductStatus> = setOf(ProductStatus.ACTIVE),
    val searchStrategy: SearchStrategy = SearchStrategy.NONE
) {
    init {
        require(statusList.isNotEmpty()) {
            "statusList cannot be empty. It must contain at least one status value."
        }
    }
}
