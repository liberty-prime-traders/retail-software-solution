package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleConfirmationHandler
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDraftHandler
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleNotesUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdater
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleVoidCreateDto
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
    private val saleDraftHandler: SaleDraftHandler,
    private val saleConfirmationHandler: SaleConfirmationHandler,
    private val saleDataFetcher: SaleDataFetcher,
    private val saleUpdater: SaleUpdater,
) {

    @GetMapping
    fun fetchRecent(@RequestParam n: Int?): List<SaleResponseDto> {
        return saleDataFetcher.fetchRecent(n)
    }

    @PostMapping("draft")
    fun createDraft(@RequestBody dto: SaleCreateDto): SaleResponseDto {
        return saleDraftHandler.createDraft(dto)
    }

    @PutMapping("draft")
    fun updateDraft(@RequestBody dto: SaleUpdateDto): SaleResponseDto {
        return saleDraftHandler.updateDraft(dto)
    }

    @PutMapping("complete")
    fun convertDraftToSale(@RequestBody dto: SaleUpdateDto): SaleResponseDto {
        return saleConfirmationHandler.convertDraftToSale(dto)
    }

    @PostMapping("complete")
    fun createSale(@RequestBody dto: SaleCreateDto): SaleResponseDto {
        return saleConfirmationHandler.createSale(dto)
    }

    @PutMapping("void")
    fun voidSale(@RequestBody dto: SaleVoidCreateDto): SaleResponseDto {
        return saleUpdater.voidSale(dto)
    }

    @PutMapping("{saleId}/notes")
    fun updateNotes(@PathVariable saleId: UUID, @RequestBody dto: SaleNotesUpdateDto): ResponseEntity<Unit> {
        saleUpdater.updateNotes(saleId, dto.notes)
        return ResponseEntity.ok().build()
    }
}
