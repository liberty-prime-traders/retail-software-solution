package me.ezra_home.retail_software_solution.util.paging

data class PageResponseWithMetadata<CONTENT, CURSOR, METADATA>(
    val currentCursor: CURSOR,
    val hasMore: Boolean,
    val contents: Collection<CONTENT>,
    val metadata: METADATA,
    var requireClientSideFilter: Boolean = false
)
