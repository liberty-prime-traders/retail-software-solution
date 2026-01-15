package me.ezra_home.retail_software_solution.util.paging

data class PageRequest<PARAMETER> (
    val previousCursor: Long,
    val requestedSize: Int,
    val parameters: PARAMETER
)
