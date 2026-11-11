package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.stock_transfer.api.StockTransferCreateDto
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.api.StockTransferDispatchService
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.api.StockTransferDraftService
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.api.StockTransferLineRequestDto
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.api.StockTransferReceiptService
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.api.StockTransferResponse
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.api.StockTransferService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferSummaryDto
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferSummaryService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/stock-transfers")
class StockTransferEndpoint(
    private val stockTransferService: StockTransferService,
    private val stockTransferDraftService: StockTransferDraftService,
    private val stockTransferDispatchService: StockTransferDispatchService,
    private val stockTransferReceiptService: StockTransferReceiptService,
    private val stockTransferSummaryService: StockTransferSummaryService
) {

    @PostMapping("draft")
    fun createTransfer(@RequestBody stockTransferCreateDto: StockTransferCreateDto): StockTransferResponse =
        stockTransferService.createTransfer(stockTransferCreateDto)

    @PostMapping("{orderReferenceNumber}/dispatch")
    fun dispatch(@PathVariable orderReferenceNumber: String): StockTransferResponse =
        stockTransferDispatchService.dispatch(orderReferenceNumber)

    @PostMapping("{orderReferenceNumber}/cancel")
    fun cancelTransfer(@PathVariable orderReferenceNumber: String): StockTransferResponse =
        stockTransferService.cancelTransfer(orderReferenceNumber)

    @PostMapping("{orderReferenceNumber}/lines")
    fun applyLineChanges(
        @PathVariable orderReferenceNumber: String,
        @RequestBody lineRequestDto: StockTransferLineRequestDto
    ): StockTransferResponse =
        stockTransferDraftService.applyLineChanges(orderReferenceNumber, lineRequestDto)

    @PostMapping("{orderReferenceNumber}/lines/remove/{lineRef}")
    fun removeLine(
        @PathVariable orderReferenceNumber: String,
        @PathVariable lineRef: String
    ): StockTransferResponse =
        stockTransferDraftService.removeLine(orderReferenceNumber, lineRef)

    @PostMapping("{orderReferenceNumber}/lines/confirm/{dispatchLineReferenceNumber}")
    fun confirmLine(
        @PathVariable orderReferenceNumber: String,
        @PathVariable dispatchLineReferenceNumber: String
    ): StockTransferResponse =
        stockTransferReceiptService.confirmLine(orderReferenceNumber, dispatchLineReferenceNumber)

    @DeleteMapping("{orderReferenceNumber}/lines/unconfirm/{dispatchLineReferenceNumber}")
    fun unconfirmLine(
        @PathVariable orderReferenceNumber: String,
        @PathVariable dispatchLineReferenceNumber: String
    ): StockTransferResponse =
        stockTransferReceiptService.unconfirmLine(orderReferenceNumber, dispatchLineReferenceNumber)

    @PostMapping("{receiptReferenceNumber}/complete")
    fun completeTransfer(@PathVariable receiptReferenceNumber: String): StockTransferResponse =
        stockTransferReceiptService.completeTransfer(receiptReferenceNumber)

    @GetMapping
    fun fetchTop(@RequestParam n: Int?): List<StockTransferResponse> =
        stockTransferService.fetchTop(n ?: 50)

    @GetMapping("{orderReferenceNumber}")
    fun fetchByOrderReferenceNumber(@PathVariable orderReferenceNumber: String): StockTransferResponse =
        stockTransferService.fetchByOrderReferenceNumber(orderReferenceNumber)

    @GetMapping("summary")
    fun fetchSummary(@RequestParam n: Int?): List<StockTransferSummaryDto> =
        stockTransferSummaryService.fetchByLocation( n ?: 50)

    @GetMapping("summary/for-organization")
    fun fetchSummaryForOrganization(@RequestParam n: Int?): List<StockTransferSummaryDto> =
        stockTransferSummaryService.fetchForOrganization(n ?: 50)
}
