package me.ezra_home.retail_software_solution.util.paging

data class PageResponse<CONTENT>(
    val currentCursor: Long,
    val hasMore: Boolean,
    val contents: Collection<CONTENT>
)
