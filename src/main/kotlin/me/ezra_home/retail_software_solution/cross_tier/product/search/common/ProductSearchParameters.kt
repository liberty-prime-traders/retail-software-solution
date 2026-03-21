package me.ezra_home.retail_software_solution.cross_tier.product.search.common

import me.ezra_home.retail_software_solution.organizations.business.product.ProductStatus
import me.ezra_home.retail_software_solution.util.queries.HasSearchStrategy
import me.ezra_home.retail_software_solution.util.queries.SearchStrategy
import java.util.UUID

data class ProductSearchParameters(
    val searchText: String? = null,
    val referenceNumber: String? = null,
    val categoryIds: Set<UUID> = emptySet(),
    val tagIds: Set<UUID> = emptySet(),
    val statusList: Set<ProductStatus> = setOf(ProductStatus.ACTIVE),
    override val searchStrategy: SearchStrategy = SearchStrategy.NONE
): HasSearchStrategy<ProductSearchParameters> {

    init {
        require(statusList.isNotEmpty()) {
          "statusList cannot be empty. It must contain at least one status value."
        }
    }

    fun extractStatusCodes(): Set<String> {
        return statusList.map { it.code }.toSet()
    }

    override fun copySelf(searchStrategy: SearchStrategy): ProductSearchParameters {
        return copy(searchStrategy = searchStrategy)
    }
}
