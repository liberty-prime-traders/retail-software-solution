package me.ezra_home.retail_software_solution.locations.business.expense.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseSummaryResponse
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpensePaymentCreateRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpensePaymentVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.PurchaseExpenseBatchRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.SaleExpenseBatchRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.StandaloneExpenseBatchRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.WageExpenseBatchRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.operation.ExpenseOperations
import me.ezra_home.retail_software_solution.cross_tier.expense.operation.ExpensePaymentOperations
import me.ezra_home.retail_software_solution.cross_tier.expense.operation.ExpenseReissuer
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseRowCommand
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeService
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.SystemExpenseType
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class ExpenseService(
    private val expenseOperations: ExpenseOperations,
    private val expensePaymentOperations: ExpensePaymentOperations,
    private val locationExpenseStore: LocationExpenseStore,
    private val expenseTypeService: ExpenseTypeService,
    private val purchaseDataFetcher: PurchaseDataFetcher,
    private val saleDataFetcher: SaleDataFetcher
) : ExpenseReissuer {

    override val isLocationLevel = true

    fun createStandalone(standaloneExpenseBatchRequest: StandaloneExpenseBatchRequest): List<ExpenseSummaryResponse> =
        expenseOperations.createStandalone(locationExpenseStore, standaloneExpenseBatchRequest)

    fun createWages(wageExpenseBatchRequest: WageExpenseBatchRequest): List<ExpenseSummaryResponse> {
        val wageExpenseTypeId = expenseTypeService.getBySystemExpenseType(SystemExpenseType.WAGES).id
        return expenseOperations.createBatch(
            locationExpenseStore,
            ExpenseSourceType.WAGES,
            "${ExpenseSourceType.WAGES.batchDescriptionLabel()} – ${wageExpenseBatchRequest.expenseDate}",
            wageExpenseBatchRequest.expenseDate,
            wageExpenseBatchRequest.rows.map {
                ExpenseRowCommand(wageExpenseTypeId, it.employeeContactId, it.amount, null, it.expenseDateOverride, it.settlement)
            }
        )
    }

    fun createForPurchase(purchaseExpenseBatchRequest: PurchaseExpenseBatchRequest): List<ExpenseSummaryResponse> {
        val purchaseInfo = purchaseDataFetcher.findPurchaseInfoByReferenceNumber(
            StringUtils.getValueOrException(purchaseExpenseBatchRequest.purchaseReference, "Purchase reference is required")
        )
        if (purchaseInfo.purchaseStatus == PurchaseStatus.DRAFT) {
            throw RtsGenericException("Purchase ${purchaseInfo.referenceNumber} is still a draft and cannot carry expenses")
        }
        return expenseOperations.createForSourceDocument(
            locationExpenseStore,
            ExpenseSourceType.PURCHASE,
            purchaseInfo.referenceNumber,
            purchaseInfo.id,
            purchaseExpenseBatchRequest.expenseDate,
            purchaseExpenseBatchRequest.rows.map {
                ExpenseRowCommand(
                    it.expenseTypeId, it.payeeContactId ?: purchaseInfo.supplierId, it.amount,
                    it.description, it.expenseDateOverride, it.settlement
                )
            }
        )
    }

    fun createForSale(saleExpenseBatchRequest: SaleExpenseBatchRequest): List<ExpenseSummaryResponse> {
        val saleHeader = saleDataFetcher.getSaleHeaderByReferenceNumber(
            StringUtils.getValueOrException(saleExpenseBatchRequest.saleReference, "Sale reference is required")
        )
        when (saleHeader.status) {
            SaleStatus.DRAFT -> throw RtsGenericException("Sale ${saleHeader.referenceNumber} is still a draft and cannot carry expenses")
            SaleStatus.DISCARDED -> throw RtsGenericException("Sale ${saleHeader.referenceNumber} was discarded and cannot carry expenses")
            SaleStatus.CONFIRMED, SaleStatus.VOIDED -> Unit
        }
        return expenseOperations.createForSourceDocument(
            locationExpenseStore,
            ExpenseSourceType.SALE,
            saleHeader.referenceNumber,
            saleHeader.id,
            saleExpenseBatchRequest.expenseDate,
            saleExpenseBatchRequest.rows.map { it.toRowCommand() }
        )
    }

    fun recordPayments(expensePaymentCreateRequests: List<ExpensePaymentCreateRequest>): List<ExpenseSummaryResponse> =
        expensePaymentOperations.recordPayments(locationExpenseStore, expensePaymentCreateRequests)

    fun voidExpense(expenseVoidRequest: ExpenseVoidRequest): ExpenseSummaryResponse =
        expenseOperations.voidExpense(locationExpenseStore, expenseVoidRequest)

    fun voidPayment(expensePaymentVoidRequest: ExpensePaymentVoidRequest): ExpenseSummaryResponse =
        expensePaymentOperations.voidPayment(locationExpenseStore, expensePaymentVoidRequest)

    @TransactionalOnLocationSchema(readOnly = true)
    fun getExpense(expenseReference: String): ExpenseSummaryResponse =
        expenseOperations.getExpense(locationExpenseStore, expenseReference)

    @TransactionalOnLocationSchema(readOnly = true)
    fun getBySource(sourceType: ExpenseSourceType, sourceReference: String): List<ExpenseSummaryResponse> =
        expenseOperations.getBySource(locationExpenseStore, sourceType, sourceReference)

    @TransactionalOnLocationSchema(readOnly = true)
    fun getRecent(limit: Int): List<ExpenseSummaryResponse> = expenseOperations.getRecent(locationExpenseStore, limit)

    @TransactionalOnLocationSchema(readOnly = true)
    override fun reissueRecorded(expenseId: UUID) = expenseOperations.reissueRecorded(locationExpenseStore, expenseId)

    @TransactionalOnLocationSchema(readOnly = true)
    override fun reissueVoided(expenseVoidId: UUID) = expenseOperations.reissueVoided(locationExpenseStore, expenseVoidId)

    @TransactionalOnLocationSchema(readOnly = true)
    override fun reissuePaymentRecorded(paymentId: UUID) = expensePaymentOperations.reissuePaymentRecorded(locationExpenseStore, paymentId)

    @TransactionalOnLocationSchema(readOnly = true)
    override fun reissuePaymentVoided(paymentVoidId: UUID) = expensePaymentOperations.reissuePaymentVoided(locationExpenseStore, paymentVoidId)
}
