package me.ezra_home.retail_software_solution.util.queries

data class QueryMetadata(
  val queryName: String = "product_search",
  val categoryIdsCount: Int = 0,
  val statusListCount: Int = 0,
  val hasTextSearch: Boolean = false,
  val tagIdsCount: Int? = null,
  val hasReferenceNumberSearch: Boolean? = null,
  val hasTagFilter: Boolean? = null
)
