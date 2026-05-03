package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCreator
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdater
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
    private val saleCreator: SaleCreator,
    private val saleDataFetcher: SaleDataFetcher,
    private val saleUpdater: SaleUpdater,
) {

    @GetMapping
    fun fetchTopN(@RequestParam n: Int?): List<SaleResponseDto> {
        return saleDataFetcher.fetchTopN(n)
    }

    @PostMapping("draft")
    fun createDraft(@RequestBody dto: SaleCreateDto): SaleResponseDto {
        return saleCreator.createDraft(dto)
    }

    @PutMapping("draft")
    fun updateDraft(@RequestBody dto: SaleUpdateDto): SaleResponseDto {
        return saleCreator.updateDraft(dto)
    }

    @PutMapping("{saleId}/void")
    fun voidSale(@PathVariable saleId: UUID): SaleResponseDto {
        return saleUpdater.voidSale(saleId)
    }
}
