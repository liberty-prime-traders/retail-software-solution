package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpensePaymentCreateRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpensePaymentVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseSummaryResponse
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.PurchaseExpenseBatchRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.SaleExpenseBatchRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.StandaloneExpenseBatchRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.WageExpenseBatchRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchParameters
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchSummaryResponseDto
import me.ezra_home.retail_software_solution.locations.business.expense.api.ExpenseSearchService
import me.ezra_home.retail_software_solution.locations.business.expense.api.ExpenseService
import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import me.ezra_home.retail_software_solution.util.annotations.RequiresFeature
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/expenses")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class ExpenseEndpoint(
    private val expenseService: ExpenseService,
    private val expenseSearchService: ExpenseSearchService
) {

    @PostMapping("standalone")
    fun createStandalone(@RequestBody standaloneExpenseBatchRequest: StandaloneExpenseBatchRequest): List<ExpenseSummaryResponse> =
        expenseService.createStandalone(standaloneExpenseBatchRequest)

    @PostMapping("wages")
    fun createWages(@RequestBody wageExpenseBatchRequest: WageExpenseBatchRequest): List<ExpenseSummaryResponse> =
        expenseService.createWages(wageExpenseBatchRequest)

    @PostMapping("purchase")
    fun createForPurchase(@RequestBody purchaseExpenseBatchRequest: PurchaseExpenseBatchRequest): List<ExpenseSummaryResponse> =
        expenseService.createForPurchase(purchaseExpenseBatchRequest)

    @PostMapping("sale")
    fun createForSale(@RequestBody saleExpenseBatchRequest: SaleExpenseBatchRequest): List<ExpenseSummaryResponse> =
        expenseService.createForSale(saleExpenseBatchRequest)

    @PostMapping("payments")
    fun recordPayments(@RequestBody expensePaymentCreateRequests: List<ExpensePaymentCreateRequest>): List<ExpenseSummaryResponse> =
        expenseService.recordPayments(expensePaymentCreateRequests)

    @PostMapping("payments/void")
    fun voidPayment(@RequestBody expensePaymentVoidRequest: ExpensePaymentVoidRequest): ExpenseSummaryResponse =
        expenseService.voidPayment(expensePaymentVoidRequest)

    @PostMapping("void")
    fun voidExpense(@RequestBody expenseVoidRequest: ExpenseVoidRequest): ExpenseSummaryResponse =
        expenseService.voidExpense(expenseVoidRequest)

    @PostMapping("search")
    fun search(@RequestBody pageRequest: PageRequest<ExpenseSearchParameters, String>): PageResponse<ExpenseSummaryResponse, String> =
        expenseSearchService.search(pageRequest)

    @PostMapping("search/summary")
    fun searchSummary(@RequestBody expenseSearchParameters: ExpenseSearchParameters): ExpenseSearchSummaryResponseDto =
        expenseSearchService.summarize(expenseSearchParameters)

    @GetMapping("recent")
    fun getRecent(@RequestParam(defaultValue = "50") limit: Int): List<ExpenseSummaryResponse> =
        expenseService.getRecent(limit)

    @GetMapping("by-source")
    fun getBySource(
        @RequestParam sourceType: ExpenseSourceType,
        @RequestParam sourceReference: String
    ): List<ExpenseSummaryResponse> = expenseService.getBySource(sourceType, sourceReference)

    @GetMapping("{expenseReference}")
    fun getExpense(@PathVariable expenseReference: String): ExpenseSummaryResponse =
        expenseService.getExpense(expenseReference)
}
