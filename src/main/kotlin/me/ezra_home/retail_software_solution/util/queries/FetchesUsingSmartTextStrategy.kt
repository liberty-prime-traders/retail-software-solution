package me.ezra_home.retail_software_solution.util.queries

import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse

interface FetchesUsingSmartTextStrategy<PARAMETERS, RESPONSE_TYPE> where PARAMETERS : HasSearchStrategy<PARAMETERS> {
    fun fetch(
        pageRequest: PageRequest<PARAMETERS, String>,
        setTimeout: Boolean
    ): PageResponse<RESPONSE_TYPE, String>
}
