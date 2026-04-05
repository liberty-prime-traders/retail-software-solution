package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseCancelLinesDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseNotesUpdateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseResponseDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseUpdateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseSearchService
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
@RequestMapping("secured/purchases")
class PurchaseEndpoint(
  private val purchaseService: PurchaseService,
  private val purchaseSearchService: PurchaseSearchService
) {

  @PostMapping("draft")
  fun createDraft(@RequestBody dto: PurchaseCreateDto): PurchaseResponseDto =
    purchaseService.createDraft(dto)

  @PutMapping("draft")
  fun updateDraft(@RequestBody dto: PurchaseUpdateDto): PurchaseResponseDto =
    purchaseService.updateDraft(dto)

  @PostMapping("order")
  fun createOrder(@RequestBody dto: PurchaseCreateDto): PurchaseResponseDto =
    purchaseService.createOrder(dto)

  @PutMapping("order")
  fun convertDraftToOrder(@RequestBody dto: PurchaseUpdateDto): PurchaseResponseDto =
    purchaseService.convertDraftToOrder(dto)

  @PutMapping("{id}/line-cancel-quantities")
  fun cancelLines(@PathVariable id: UUID, @RequestBody lines: List<PurchaseCancelLinesDto>): PurchaseResponseDto =
    purchaseService.updateCancelQuantities(id, lines)

  @PutMapping("{id}/notes")
  fun updateNotes(@PathVariable id: UUID, @RequestBody dto: PurchaseNotesUpdateDto): ResponseEntity<Unit> {
    purchaseService.updateNotes(id, dto.notes)
    return ResponseEntity.ok().build()
  }

  @GetMapping
  fun fetchTop(@RequestParam n: Int?): List<PurchaseResponseDto> =
    purchaseSearchService.fetchTop(n)

//  @PostMapping("search")
//  fun search(@RequestBody request: PurchaseSearchRequest): List<PurchaseResponseDto> =
//    purchaseSearchService.search(request)
}
