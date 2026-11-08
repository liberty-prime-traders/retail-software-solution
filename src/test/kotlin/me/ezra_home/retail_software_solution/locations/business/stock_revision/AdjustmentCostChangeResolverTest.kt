package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentAccountCodes
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentAccountCodesFetcher
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.AdjustmentCostChange
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.PostedStockAdjustmentAccounts
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.PostedStockAdjustmentAccountsReader
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal

class AdjustmentCostChangeResolverTest {

    private val stockAdjustmentAccountCodesFetcher = mock(StockAdjustmentAccountCodesFetcher::class.java).also {
        `when`(it.getByReferenceNumber(listOf("ADJ1"))).thenReturn(
            mapOf("ADJ1" to StockAdjustmentAccountCodes("RULE-DR", "RULE-CR"))
        )
    }
    private val postedStockAdjustmentAccountsReader = mock(PostedStockAdjustmentAccountsReader::class.java)
    private val adjustmentCostChangeResolver = AdjustmentCostChangeResolver(
        StockAdjustmentRestatements(stockAdjustmentAccountCodesFetcher), postedStockAdjustmentAccountsReader
    )

    private fun costChange(movementType: MovementType, documentReference: String, oldCost: String, newCost: String) =
        ConsumerCostChange(movementType, documentReference, null, BigDecimal(oldCost), BigDecimal(newCost))

    @Test
    fun `an adjustment uses the accounts its original posting used, not the ones the rules give today`() {
        `when`(postedStockAdjustmentAccountsReader.findByAdjustmentReferenceNumber(setOf("ADJ1"))).thenReturn(
            mapOf("ADJ1" to PostedStockAdjustmentAccounts("POSTED-DR", "POSTED-CR"))
        )

        val adjustmentCostChanges = adjustmentCostChangeResolver.byAdjustmentReference(
            listOf(costChange(MovementType.STOCK_ADJUSTMENT_DECREASE, "ADJ1", "10.00", "9.00"))
        )

        assertEquals(
            mapOf("ADJ1" to AdjustmentCostChange("POSTED-DR", "POSTED-CR", BigDecimal("-1.00"))),
            adjustmentCostChanges
        )
    }

    @Test
    fun `an adjustment with no posting to read uses the rule accounts`() {
        `when`(postedStockAdjustmentAccountsReader.findByAdjustmentReferenceNumber(setOf("ADJ1")))
            .thenReturn(emptyMap())

        val adjustmentCostChanges = adjustmentCostChangeResolver.byAdjustmentReference(
            listOf(costChange(MovementType.STOCK_ADJUSTMENT_DECREASE, "ADJ1", "10.00", "12.50"))
        )

        assertEquals(
            mapOf("ADJ1" to AdjustmentCostChange("RULE-DR", "RULE-CR", BigDecimal("2.50"))),
            adjustmentCostChanges
        )
    }

    @Test
    fun `sales and adjustments whose cost did not move resolve to nothing without reading the ledger`() {
        val adjustmentCostChanges = adjustmentCostChangeResolver.byAdjustmentReference(
            listOf(
                costChange(MovementType.SALE, "SAL1", "40.00", "48.00"),
                costChange(MovementType.STOCK_ADJUSTMENT_DECREASE, "ADJ1", "10.00", "10.00"),
            )
        )

        assertTrue(adjustmentCostChanges.isEmpty())
        verifyNoInteractions(postedStockAdjustmentAccountsReader)
    }
}
