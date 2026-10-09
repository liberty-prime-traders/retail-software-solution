package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.request.ExpensePaymentCreateRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.request.ExpensePaymentVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.response.ExpenseSummaryResponse
import me.ezra_home.retail_software_solution.cross_tier.expense.request.ExpenseVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.request.StandaloneExpenseBatchRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.request.StockTransferExpenseBatchRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchParameters
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchSummaryResponseDto
import me.ezra_home.retail_software_solution.organizations.business.org_expense.api.OrgExpenseSearchService
import me.ezra_home.retail_software_solution.organizations.business.org_expense.api.OrgExpenseService
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
@RequestMapping("secured/org-expenses")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class OrgExpenseEndpoint(
    private val orgExpenseService: OrgExpenseService,
    private val orgExpenseSearchService: OrgExpenseSearchService
) {

    @PostMapping("standalone")
    fun createStandalone(@RequestBody standaloneExpenseBatchRequest: StandaloneExpenseBatchRequest): List<ExpenseSummaryResponse> =
        orgExpenseService.createStandalone(standaloneExpenseBatchRequest)

    @PostMapping("stock-transfer")
    fun createForStockTransfer(
        @RequestBody stockTransferExpenseBatchRequest: StockTransferExpenseBatchRequest
    ): List<ExpenseSummaryResponse> = orgExpenseService.createForStockTransfer(stockTransferExpenseBatchRequest)

    @PostMapping("search")
    fun search(@RequestBody pageRequest: PageRequest<ExpenseSearchParameters, String>): PageResponse<ExpenseSummaryResponse, String> =
        orgExpenseSearchService.search(pageRequest)

    @PostMapping("search/summary")
    fun searchSummary(@RequestBody expenseSearchParameters: ExpenseSearchParameters): ExpenseSearchSummaryResponseDto =
        orgExpenseSearchService.summarize(expenseSearchParameters)

    @PostMapping("payments")
    fun recordPayments(@RequestBody expensePaymentCreateRequests: List<ExpensePaymentCreateRequest>): List<ExpenseSummaryResponse> =
        orgExpenseService.recordPayments(expensePaymentCreateRequests)

    @PostMapping("payments/void")
    fun voidPayment(@RequestBody expensePaymentVoidRequest: ExpensePaymentVoidRequest): ExpenseSummaryResponse =
        orgExpenseService.voidPayment(expensePaymentVoidRequest)

    @PostMapping("void")
    fun voidExpense(@RequestBody expenseVoidRequest: ExpenseVoidRequest): ExpenseSummaryResponse =
        orgExpenseService.voidExpense(expenseVoidRequest)

    @GetMapping("by-source")
    fun getBySource(
        @RequestParam sourceType: ExpenseSourceType,
        @RequestParam sourceReference: String
    ): List<ExpenseSummaryResponse> = orgExpenseService.getBySource(sourceType, sourceReference)

    @GetMapping("{expenseReference}")
    fun getExpense(@PathVariable expenseReference: String): ExpenseSummaryResponse =
        orgExpenseService.getExpense(expenseReference)
}
