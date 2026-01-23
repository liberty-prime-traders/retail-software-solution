package me.ezra_home.retail_software_solution.util.paging

data class PageResponse<CONTENT, CURSOR>(
    val currentCursor: CURSOR,
    val hasMore: Boolean,
    val contents: Collection<CONTENT>,
    var requireClientSideFilter: Boolean = false
)
