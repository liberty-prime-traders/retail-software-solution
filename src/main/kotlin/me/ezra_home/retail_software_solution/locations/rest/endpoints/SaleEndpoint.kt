package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleNotesUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSearchParameters
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSearchService
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSearchSummaryResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSummary
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdater
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/sales")
class SaleEndpoint(
    private val saleDataFetcher: SaleDataFetcher,
    private val saleUpdater: SaleUpdater,
    private val saleSearchService: SaleSearchService,
) {

    @GetMapping
    fun fetchRecent(@RequestParam n: Int?): List<SaleSummary> {
        return saleDataFetcher.fetchRecent(n)
    }

    @PutMapping("{saleId}/notes")
    fun updateNotes(@PathVariable saleId: UUID, @RequestBody dto: SaleNotesUpdateDto): ResponseEntity<Unit> {
        saleUpdater.updateNotes(saleId, dto.notes)
        return ResponseEntity.ok().build()
    }

    @PostMapping("search")
    fun search(@RequestBody pageRequest: PageRequest<SaleSearchParameters, String>): PageResponse<SaleSummary, String> {
        return saleSearchService.search(pageRequest)
    }

    @PostMapping("search/summary")
    fun searchSummary(@RequestBody saleSearchParameters: SaleSearchParameters): SaleSearchSummaryResponseDto {
        return saleSearchService.summarize(saleSearchParameters)
    }
}
