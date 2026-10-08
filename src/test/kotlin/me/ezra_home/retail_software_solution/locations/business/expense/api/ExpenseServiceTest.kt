package me.ezra_home.retail_software_solution.locations.business.expense.api

import me.ezra_home.retail_software_solution.cross_tier.expense.operation.ExpenseOperations
import me.ezra_home.retail_software_solution.cross_tier.expense.operation.ExpensePaymentOperations
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseRowCommand
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.api.PurchaseExpenseBatchRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.PurchaseExpenseRowRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.RequiredPayeeExpenseRowRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.SaleExpenseBatchRequest
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleHeaderDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class ExpenseServiceTest {

    private val expenseOperations = mock(ExpenseOperations::class.java)
    private val expensePaymentOperations = mock(ExpensePaymentOperations::class.java)
    private val locationExpenseStore = mock(LocationExpenseStore::class.java)
    private val purchaseDataFetcher = mock(PurchaseDataFetcher::class.java)
    private val saleDataFetcher = mock(SaleDataFetcher::class.java)

    private val expenseService = ExpenseService(
        expenseOperations, expensePaymentOperations, locationExpenseStore, mock(ExpenseTypeService::class.java), purchaseDataFetcher, saleDataFetcher
    )

    private val expenseDate = LocalDate.of(2026, 3, 10)
    private val expenseTypeId = UUID.randomUUID()
    private val explicitPayeeContactId = UUID.randomUUID()
    private val supplierId = UUID.randomUUID()

    @Test
    fun `a draft purchase cannot carry expenses`() {
        stubPurchase(PurchaseStatus.DRAFT)

        assertThrows(RtsGenericException::class.java) { expenseService.createForPurchase(purchaseRequest()) }

        verifyNoInteractions(expenseOperations)
    }

    @Test
    fun `a cancelled purchase can still carry expenses and the supplier is the default payee`() {
        val purchaseId = stubPurchase(PurchaseStatus.CANCELED)
        val expectedCommands = listOf(
            ExpenseRowCommand(expenseTypeId, supplierId, BigDecimal.TEN, null, null, null),
            ExpenseRowCommand(expenseTypeId, explicitPayeeContactId, BigDecimal.ONE, null, null, null)
        )
        `when`(
            expenseOperations.createForSourceDocument(
                locationExpenseStore, ExpenseSourceType.PURCHASE, "PRCH01", purchaseId, expenseDate, expectedCommands
            )
        ).thenReturn(emptyList())

        expenseService.createForPurchase(purchaseRequest())

        verify(expenseOperations).createForSourceDocument(
            locationExpenseStore, ExpenseSourceType.PURCHASE, "PRCH01", purchaseId, expenseDate, expectedCommands
        )
    }

    @Test
    fun `a draft or discarded sale cannot carry expenses but a voided one can`() {
        stubSale(SaleStatus.DRAFT)
        assertThrows(RtsGenericException::class.java) { expenseService.createForSale(saleRequest()) }

        stubSale(SaleStatus.DISCARDED)
        assertThrows(RtsGenericException::class.java) { expenseService.createForSale(saleRequest()) }
        verifyNoInteractions(expenseOperations)

        val saleId = stubSale(SaleStatus.VOIDED)
        val expectedCommands = listOf(ExpenseRowCommand(expenseTypeId, explicitPayeeContactId, BigDecimal.TEN, null, null, null))
        `when`(
            expenseOperations.createForSourceDocument(
                locationExpenseStore, ExpenseSourceType.SALE, "SALE01", saleId, expenseDate, expectedCommands
            )
        ).thenReturn(emptyList())

        expenseService.createForSale(saleRequest())

        verify(expenseOperations).createForSourceDocument(
            locationExpenseStore, ExpenseSourceType.SALE, "SALE01", saleId, expenseDate, expectedCommands
        )
    }

    private fun stubPurchase(status: PurchaseStatus): UUID {
        val purchaseId = UUID.randomUUID()
        `when`(purchaseDataFetcher.findPurchaseInfoByReferenceNumber("PRCH01")).thenReturn(
            PurchaseDataFetcher.PurchaseInfo(purchaseId, "PRCH01", supplierId, status, PaymentStatus.UNPAID)
        )
        return purchaseId
    }

    private fun stubSale(status: SaleStatus): UUID {
        val saleId = UUID.randomUUID()
        `when`(saleDataFetcher.getSaleHeaderByReferenceNumber("SALE01")).thenReturn(
            SaleHeaderDto(saleId, "SALE01", 0, status, UUID.randomUUID(), null, null, null)
        )
        return saleId
    }

    private fun purchaseRequest() = PurchaseExpenseBatchRequest(
        "PRCH01", expenseDate,
        listOf(
            PurchaseExpenseRowRequest(expenseTypeId = expenseTypeId, amount = BigDecimal.TEN),
            PurchaseExpenseRowRequest(payeeContactId = explicitPayeeContactId, expenseTypeId = expenseTypeId, amount = BigDecimal.ONE)
        )
    )

    private fun saleRequest() = SaleExpenseBatchRequest(
        "SALE01", expenseDate,
        listOf(RequiredPayeeExpenseRowRequest(payeeContactId = explicitPayeeContactId, expenseTypeId = expenseTypeId, amount = BigDecimal.TEN))
    )
}
