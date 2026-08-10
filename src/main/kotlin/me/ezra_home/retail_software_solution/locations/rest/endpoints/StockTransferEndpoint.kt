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
import org.springframework.web.bind.annotation.PutMapping
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

    @PostMapping("{orderRef}/dispatch")
    fun dispatch(@PathVariable orderRef: String): StockTransferResponse =
        stockTransferDispatchService.dispatch(orderRef)

    @PutMapping("{orderRef}/cancel")
    fun cancelTransfer(@PathVariable orderRef: String): StockTransferResponse =
        stockTransferService.cancelTransfer(orderRef)

    @PutMapping("{orderRef}/lines")
    fun applyLineChanges(
        @PathVariable orderRef: String,
        @RequestBody lineRequestDto: StockTransferLineRequestDto
    ): StockTransferResponse =
        stockTransferDraftService.applyLineChanges(orderRef, lineRequestDto)

    @PutMapping("{orderRef}/lines/remove/{lineRef}")
    fun removeLine(
        @PathVariable orderRef: String,
        @PathVariable lineRef: String
    ): StockTransferResponse =
        stockTransferDraftService.removeLine(orderRef, lineRef)

    @PostMapping("{orderRef}/lines/{dispatchLineRef}/confirm")
    fun confirmLine(
        @PathVariable orderRef: String,
        @PathVariable dispatchLineRef: String
    ): StockTransferResponse =
        stockTransferReceiptService.confirmLine(orderRef, dispatchLineRef)

    @DeleteMapping("{orderRef}/lines/{dispatchLineRef}/confirm")
    fun unconfirmLine(
        @PathVariable orderRef: String,
        @PathVariable dispatchLineRef: String
    ): StockTransferResponse =
        stockTransferReceiptService.unconfirmLine(orderRef, dispatchLineRef)

    @PostMapping("{receiptRef}/complete")
    fun completeReceipt(@PathVariable receiptRef: String): StockTransferResponse =
        stockTransferReceiptService.completeReceipt(receiptRef)

    @GetMapping
    fun fetchTop(@RequestParam n: Int?): List<StockTransferResponse> =
        stockTransferService.fetchTop(n ?: 50)

    @GetMapping("{orderRef}")
    fun fetchByOrderRef(@PathVariable orderRef: String): StockTransferResponse =
        stockTransferService.fetchByOrderRef(orderRef)

    @GetMapping("summary")
    fun fetchSummary(@RequestParam n: Int?): List<StockTransferSummaryDto> =
        stockTransferSummaryService.fetchByLocation( n ?: 50)

    @GetMapping("summary/for-organization")
    fun fetchSummaryForOrganization(@RequestParam n: Int?): List<StockTransferSummaryDto> =
        stockTransferSummaryService.fetchForOrganization(n ?: 50)
}
