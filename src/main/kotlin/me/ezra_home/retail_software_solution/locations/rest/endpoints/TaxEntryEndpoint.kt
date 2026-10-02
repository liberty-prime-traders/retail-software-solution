package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntrySearchParameters
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntrySearchResultDto
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntrySearchService
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntrySearchSummaryResponseDto
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/tax-entries")
class TaxEntryEndpoint(
    private val taxEntrySearchService: TaxEntrySearchService
) {

    @PostMapping("search")
    fun search(@RequestBody pageRequest: PageRequest<TaxEntrySearchParameters, String>): PageResponse<TaxEntrySearchResultDto, String> {
        return taxEntrySearchService.search(pageRequest)
    }

    @PostMapping("search/summary")
    fun searchSummary(@RequestBody taxEntrySearchParameters: TaxEntrySearchParameters): TaxEntrySearchSummaryResponseDto {
        return taxEntrySearchService.summarize(taxEntrySearchParameters)
    }
}
