package me.ezra_home.retail_software_solution.util.paging

import me.ezra_home.retail_software_solution.util.queries.HasSearchStrategy

data class PageRequest<PARAMETER: HasSearchStrategy<PARAMETER>, CURSOR>  (
    val previousCursor: CURSOR,
    val requestedSize: Int,
    val parameters: PARAMETER
) {
    companion object {
        const val REQUIRE_CLIENT_SIDE_FILTER_THRESHOLD = 1
    }
}
