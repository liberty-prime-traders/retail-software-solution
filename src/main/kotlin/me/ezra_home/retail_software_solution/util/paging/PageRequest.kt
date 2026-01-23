package me.ezra_home.retail_software_solution.util.paging

data class PageRequest<PARAMETER, CURSOR> (
    val previousCursor: CURSOR,
    val requestedSize: Int,
    val parameters: PARAMETER
)
