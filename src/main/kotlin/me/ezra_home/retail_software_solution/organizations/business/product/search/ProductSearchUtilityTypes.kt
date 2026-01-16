package me.ezra_home.retail_software_solution.organizations.business.product.search

object ProductSearchUtilityTypes {
    data class SqlQuery(
        val sql: String,
        val params: Map<String, Any>,
        val metadata: QueryMetadata
    )

    data class QueryMetadata(
        val queryName: String = "product_search",
        val categoryIdsCount: Int = 0,
        val productGroupIdsCount: Int = 0,
        val tagIdsCount: Int = 0,
        val statusListCount: Int = 0,
        val hasTextSearch: Boolean = false,
        val hasReferenceNumberSearch: Boolean = false,
        val hasTagFilter: Boolean = false
    )
}
