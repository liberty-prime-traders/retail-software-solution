package me.ezra_home.retail_software_solution.organizations.business.org_expense.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.response.ExpenseSummaryResponse
import me.ezra_home.retail_software_solution.cross_tier.expense.request.ExpensePaymentCreateRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.request.ExpensePaymentVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.request.ExpenseVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.request.StandaloneExpenseBatchRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.request.StockTransferExpenseBatchRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.operation.ExpenseOperationsFactory
import me.ezra_home.retail_software_solution.cross_tier.expense.operation.ExpenseReissuer
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseSubmission
import me.ezra_home.retail_software_solution.cross_tier.expense.model.SourceDocument
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class OrgExpenseService(
    expenseOperationsFactory: ExpenseOperationsFactory,
    orgExpenseTier: OrgExpenseTier,
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher
) : ExpenseReissuer {

    private val tierExpenseOperations = expenseOperationsFactory.operationsFor(orgExpenseTier)
    private val expenseOperations = tierExpenseOperations.expenseOperations
    private val expensePaymentOperations = tierExpenseOperations.expensePaymentOperations
    private val expenseReadOperations = tierExpenseOperations.expenseReadOperations
    private val expenseReissueOperations = tierExpenseOperations.expenseReissueOperations

    override val isLocationLevel = false

    fun createStandalone(standaloneExpenseBatchRequest: StandaloneExpenseBatchRequest): List<ExpenseSummaryResponse> =
        expenseOperations.createStandalone(standaloneExpenseBatchRequest)

    fun createForStockTransfer(stockTransferExpenseBatchRequest: StockTransferExpenseBatchRequest): List<ExpenseSummaryResponse> {
        val stockTransferOrder = stockTransferOrderDataFetcher.getByReferenceNumber(
            StringUtils.getValueOrException(stockTransferExpenseBatchRequest.transferReference, "Transfer reference is required")
        )
        if (stockTransferOrder.status == StockTransferStatus.DRAFT) {
            throw RtsGenericException("Transfer ${stockTransferOrder.referenceNumber} is still a draft and cannot carry expenses")
        }
        return expenseOperations.createForSourceDocument(
            SourceDocument(ExpenseSourceType.STOCK_TRANSFER, stockTransferOrder.referenceNumber, stockTransferOrder.id),
            ExpenseSubmission(stockTransferExpenseBatchRequest.expenseDate, stockTransferExpenseBatchRequest.rows.map { it.toRowCommand() })
        )
    }

    fun recordPayments(expensePaymentCreateRequests: List<ExpensePaymentCreateRequest>): List<ExpenseSummaryResponse> =
        expensePaymentOperations.recordPayments(expensePaymentCreateRequests)

    fun voidExpense(expenseVoidRequest: ExpenseVoidRequest): ExpenseSummaryResponse =
        expenseOperations.voidExpense(expenseVoidRequest)

    fun voidPayment(expensePaymentVoidRequest: ExpensePaymentVoidRequest): ExpenseSummaryResponse =
        expensePaymentOperations.voidPayment(expensePaymentVoidRequest)

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getExpense(expenseReference: String): ExpenseSummaryResponse =
        expenseReadOperations.getExpense(expenseReference)

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getBySource(sourceType: ExpenseSourceType, sourceReference: String): List<ExpenseSummaryResponse> =
        expenseReadOperations.getBySource(sourceType, sourceReference)

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun reissueRecorded(expenseId: UUID) = expenseReissueOperations.reissueRecorded(expenseId)

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun reissueVoided(expenseVoidId: UUID) = expenseReissueOperations.reissueVoided(expenseVoidId)

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun reissuePaymentRecorded(paymentId: UUID) = expenseReissueOperations.reissuePaymentRecorded(paymentId)

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun reissuePaymentVoided(paymentVoidId: UUID) = expenseReissueOperations.reissuePaymentVoided(paymentVoidId)
}
