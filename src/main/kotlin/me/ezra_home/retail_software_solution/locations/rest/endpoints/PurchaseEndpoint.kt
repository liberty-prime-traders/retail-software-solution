package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.purchase.api.bulk_payment.PurchaseBulkPaymentCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.bulk_payment.PurchaseBulkPaymentResponseDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.bulk_payment.PurchaseBulkPaymentSearchDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.bulk_payment.PurchaseBulkPaymentService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseCancelLinesDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseCanceller
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseNotesUpdateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseResponseDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.search.PurchaseSearchParameters
import me.ezra_home.retail_software_solution.locations.business.purchase.api.search.PurchaseSearchService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.search.PurchaseSearchSummaryDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseUpdateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseUpdater
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
@RequestMapping("secured/purchases")
class PurchaseEndpoint(
  private val purchaseService: PurchaseService,
  private val purchaseDataFetcher: PurchaseDataFetcher,
  private val purchaseCanceller: PurchaseCanceller,
  private val purchaseUpdater: PurchaseUpdater,
  private val purchaseSearchService: PurchaseSearchService,
  private val purchaseBulkPaymentService: PurchaseBulkPaymentService,
) {

  @PostMapping("bulk-payment/search")
  fun loadBulkPayment(
    @RequestBody purchaseBulkPaymentSearchDto: PurchaseBulkPaymentSearchDto
  ): List<PurchaseBulkPaymentResponseDto> = purchaseBulkPaymentService.loadPayable(purchaseBulkPaymentSearchDto)

  @PostMapping("bulk-payment")
  fun recordBulkPayment(
    @RequestBody purchaseBulkPaymentCreateDto: PurchaseBulkPaymentCreateDto
  ): List<PurchaseBulkPaymentResponseDto> = purchaseBulkPaymentService.record(purchaseBulkPaymentCreateDto)

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
    purchaseCanceller.cancel(id, lines)

  @PutMapping("{id}/notes")
  fun updateNotes(@PathVariable id: UUID, @RequestBody dto: PurchaseNotesUpdateDto): ResponseEntity<Unit> {
    purchaseUpdater.updateNotes(id, dto.notes)
    return ResponseEntity.ok().build()
  }

  @GetMapping
  fun fetchTop(@RequestParam n: Int?): List<PurchaseResponseDto> =
    purchaseDataFetcher.fetchTop(n)

  @PostMapping("search")
  fun search(
    @RequestBody pageRequest: PageRequest<PurchaseSearchParameters, String>
  ): PageResponse<PurchaseResponseDto, String> =
    purchaseSearchService.search(pageRequest)

  @PostMapping("summary")
  fun searchSummary(@RequestBody purchaseSearchParameters: PurchaseSearchParameters): PurchaseSearchSummaryDto =
    purchaseSearchService.summarize(purchaseSearchParameters)
}
