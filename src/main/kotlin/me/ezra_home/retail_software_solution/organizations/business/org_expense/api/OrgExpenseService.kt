package me.ezra_home.retail_software_solution.organizations.business.org_expense.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseSummaryResponse
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpensePaymentCreateRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpensePaymentVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.StandaloneExpenseBatchRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.StockTransferExpenseBatchRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.operation.ExpenseOperations
import me.ezra_home.retail_software_solution.cross_tier.expense.operation.ExpensePaymentOperations
import me.ezra_home.retail_software_solution.cross_tier.expense.operation.ExpenseReissuer
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class OrgExpenseService(
    private val expenseOperations: ExpenseOperations,
    private val expensePaymentOperations: ExpensePaymentOperations,
    private val orgExpenseStore: OrgExpenseStore,
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher
) : ExpenseReissuer {

    override val isLocationLevel = false

    fun createStandalone(standaloneExpenseBatchRequest: StandaloneExpenseBatchRequest): List<ExpenseSummaryResponse> =
        expenseOperations.createStandalone(orgExpenseStore, standaloneExpenseBatchRequest)

    fun createForStockTransfer(stockTransferExpenseBatchRequest: StockTransferExpenseBatchRequest): List<ExpenseSummaryResponse> {
        val stockTransferOrder = stockTransferOrderDataFetcher.getByReferenceNumber(
            StringUtils.getValueOrException(stockTransferExpenseBatchRequest.transferReference, "Transfer reference is required")
        )
        if (stockTransferOrder.status == StockTransferStatus.DRAFT) {
            throw RtsGenericException("Transfer ${stockTransferOrder.referenceNumber} is still a draft and cannot carry expenses")
        }
        return expenseOperations.createForSourceDocument(
            orgExpenseStore,
            ExpenseSourceType.STOCK_TRANSFER,
            stockTransferOrder.referenceNumber,
            stockTransferOrder.id,
            stockTransferExpenseBatchRequest.expenseDate,
            stockTransferExpenseBatchRequest.rows.map { it.toRowCommand() }
        )
    }

    fun recordPayments(expensePaymentCreateRequests: List<ExpensePaymentCreateRequest>): List<ExpenseSummaryResponse> =
        expensePaymentOperations.recordPayments(orgExpenseStore, expensePaymentCreateRequests)

    fun voidExpense(expenseVoidRequest: ExpenseVoidRequest): ExpenseSummaryResponse =
        expenseOperations.voidExpense(orgExpenseStore, expenseVoidRequest)

    fun voidPayment(expensePaymentVoidRequest: ExpensePaymentVoidRequest): ExpenseSummaryResponse =
        expensePaymentOperations.voidPayment(orgExpenseStore, expensePaymentVoidRequest)

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getExpense(expenseReference: String): ExpenseSummaryResponse =
        expenseOperations.getExpense(orgExpenseStore, expenseReference)

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getBySource(sourceType: ExpenseSourceType, sourceReference: String): List<ExpenseSummaryResponse> =
        expenseOperations.getBySource(orgExpenseStore, sourceType, sourceReference)

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getRecent(limit: Int): List<ExpenseSummaryResponse> = expenseOperations.getRecent(orgExpenseStore, limit)

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun reissueRecorded(expenseId: UUID) = expenseOperations.reissueRecorded(orgExpenseStore, expenseId)

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun reissueVoided(expenseVoidId: UUID) = expenseOperations.reissueVoided(orgExpenseStore, expenseVoidId)

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun reissuePaymentRecorded(paymentId: UUID) = expensePaymentOperations.reissuePaymentRecorded(orgExpenseStore, paymentId)

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun reissuePaymentVoided(paymentVoidId: UUID) = expensePaymentOperations.reissuePaymentVoided(orgExpenseStore, paymentVoidId)
}
