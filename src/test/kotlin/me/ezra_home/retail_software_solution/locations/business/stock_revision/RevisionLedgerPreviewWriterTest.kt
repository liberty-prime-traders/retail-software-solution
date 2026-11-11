package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.delivery.api.DeliveryLineOwnersDto
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryDataFetcher
import me.ezra_home.retail_software_solution.locations.business.delivery.api.ValidatedDeliveryLineRevisionDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseStatus
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.FifoConsumptionRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.PlannedTake
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.ConsumerReplayPlan
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.AdjustmentCostChange
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.mappers.NameResolution
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class RevisionLedgerPreviewWriterTest {

    private val unitId = UUID.randomUUID()
    private val locationProductId = UUID.randomUUID()
    private val revisedDeliveryLineId = UUID.randomUUID()
    private val purchaseId = UUID.randomUUID()
    private val supplierId = UUID.randomUUID()
    private val purchaseDeliveryDataFetcher = mock(PurchaseDeliveryDataFetcher::class.java).also {
        `when`(it.getOwners(revisedDeliveryLineId)).thenReturn(DeliveryLineOwnersDto(purchaseId, UUID.randomUUID()))
    }
    private val purchaseDataFetcher = mock(PurchaseDataFetcher::class.java).also {
        `when`(it.findPurchaseInfoById(purchaseId)).thenReturn(
            PurchaseDataFetcher.PurchaseInfo(
                purchaseId, "PCH000001", supplierId, PurchaseStatus.PARTIALLY_DELIVERED, PaymentStatus.UNPAID
            )
        )
    }
    private val nameResolution = mock(NameResolution::class.java).also {
        `when`(it.organizationContacts(listOf(supplierId))).thenReturn(mapOf(supplierId to "Ezra"))
    }
    private val revisionLedgerPreviewWriter = RevisionLedgerPreviewWriter(
        purchaseDeliveryDataFetcher, purchaseDataFetcher, nameResolution
    )

    private fun revision(
        revisedQuantity: String, revisedUnitCost: String, quantity: String, unitCost: String
    ) = ValidatedDeliveryLineRevisionDto(
        revisedDeliveryLineId = revisedDeliveryLineId,
        deliveryReferenceNumber = "DEL1",
        revisedLineReferenceNumber = "DELN1",
        purchaseLineId = UUID.randomUUID(),
        locationProductId = locationProductId,
        revisedUnitId = unitId,
        revisedStorageUnitsPerUserProvidedUnit = 1L,
        unitId = unitId,
        storageUnitsPerUserProvidedUnit = 1L,
        revisedUserProvidedQuantityDelivered = BigDecimal(revisedQuantity),
        revisedUnitCost = BigDecimal(revisedUnitCost),
        userProvidedQuantityDelivered = BigDecimal(quantity),
        unitCost = BigDecimal(unitCost),
        problems = emptyList(),
    )

    private fun plan(movementType: MovementType, documentReference: String, oldCost: String, newCost: String) =
        ConsumerReplayPlan(
            fifoConsumptionRequest = FifoConsumptionRequest(
                locationProductId = locationProductId,
                storageQuantity = ExactQuantity.ofStorage(BigDecimal.ONE),
                userProvidedQuantity = BigDecimal.ONE,
                unitId = unitId,
                storageUnitsPerUserProvidedUnit = 1L,
                movementType = movementType,
                documentReference = documentReference,
                detailReference = "${documentReference}L",
            ),
            plannedTakes = listOf(
                PlannedTake(UUID.randomUUID(), ExactQuantity.ofStorage(BigDecimal.ONE), BigDecimal(newCost))
            ),
            oldCost = BigDecimal(oldCost),
        )

    @Test
    fun `a dearer line, dearer sales and cheaper adjustments each preview the entries their posting makes`() {
        val ledgerEntries = revisionLedgerPreviewWriter.write(
            revision("10", "10.0000", "8", "15.0000"),
            listOf(
                plan(MovementType.SALE, "SAL1", "40.00", "48.00"),
                plan(MovementType.STOCK_ADJUSTMENT_DECREASE, "ADJ1", "10.00", "9.00"),
            ),
            listOf(
                AdjustmentCostChange(
                    SystemAccount.INTERNAL_USE_EXPENSE.code, SystemAccount.INVENTORY.code, BigDecimal("-1.00")
                )
            )
        )

        assertEquals(
            listOf("Purchase delivery revision", "Sale cost restatement", "Stock adjustment restatement"),
            ledgerEntries.map { it.source }
        )
        val purchaseLines = ledgerEntries[0].lines
        assertTrue(ledgerEntries[0].description.startsWith("This delivery line used to be worth "))
        assertTrue(ledgerEntries[0].description.endsWith("The amount we owe Ezra rises by the same amount."))
        assertEquals(
            listOf("Inventory (increases)", "Trade Payables (increases)"),
            purchaseLines.map { it.accountText }
        )
        assertEquals(listOf(BalanceEffect.ADDS, BalanceEffect.SUBTRACTS), purchaseLines.map { it.balanceEffect })
        assertEquals(0, BigDecimal("20.00").compareTo(purchaseLines[0].debit))
        assertNull(purchaseLines[0].credit)
        assertEquals(0, BigDecimal("20.00").compareTo(purchaseLines[1].credit))
        assertNull(purchaseLines[1].debit)
        assertEquals(
            listOf("Cost of Goods Sold (increases)", "Inventory (decreases)"),
            ledgerEntries[1].lines.map { it.accountText }
        )
        assertEquals(
            listOf(BalanceEffect.SUBTRACTS, BalanceEffect.SUBTRACTS), ledgerEntries[1].lines.map { it.balanceEffect }
        )
        assertEquals(0, BigDecimal("8.00").compareTo(ledgerEntries[1].lines[0].debit))
        assertEquals(
            listOf("Inventory (increases)", "Internal Use Expense (decreases)"),
            ledgerEntries[2].lines.map { it.accountText }
        )
        assertEquals(0, BigDecimal("1.00").compareTo(ledgerEntries[2].lines[0].debit))
        assertEquals(0, BigDecimal("1.00").compareTo(ledgerEntries[2].lines[1].credit))
    }

    @Test
    fun `an adjustment entry is built from the accounts it is given`() {
        val ledgerEntries = revisionLedgerPreviewWriter.write(
            revision("10", "10.0000", "10", "10.0000"),
            listOf(plan(MovementType.STOCK_ADJUSTMENT_DECREASE, "ADJ1", "10.00", "9.00")),
            listOf(
                AdjustmentCostChange(
                    SystemAccount.SHRINKAGE_AND_LOSSES.code, SystemAccount.INVENTORY.code, BigDecimal("-1.00")
                )
            )
        )

        assertEquals(
            listOf("Inventory (increases)", "Shrinkage & Losses (decreases)"),
            ledgerEntries.single().lines.map { it.accountText }
        )
    }

    @Test
    fun `a cheaper line posts the opposite way and a sale whose cost did not move adds no entry`() {
        val ledgerEntries = revisionLedgerPreviewWriter.write(
            revision("10", "10.0000", "10", "8.0000"),
            listOf(plan(MovementType.SALE, "SAL1", "40.00", "40.00")),
            emptyList()
        )

        assertEquals(listOf("Purchase delivery revision"), ledgerEntries.map { it.source })
        assertEquals(
            listOf("Trade Payables (decreases)", "Inventory (decreases)"),
            ledgerEntries.single().lines.map { it.accountText }
        )
        assertEquals(
            listOf(BalanceEffect.ADDS, BalanceEffect.SUBTRACTS),
            ledgerEntries.single().lines.map { it.balanceEffect }
        )
        assertEquals(0, BigDecimal("20.00").compareTo(ledgerEntries.single().lines[0].debit))
        assertTrue(ledgerEntries.single().description.endsWith("The amount we owe Ezra drops by the same amount."))
    }

    @Test
    fun `a revision that moves no value previews no entries`() {
        assertTrue(
            revisionLedgerPreviewWriter.write(revision("10", "10.0000", "10", "10.0000"), emptyList(), emptyList()).isEmpty()
        )
    }
}
