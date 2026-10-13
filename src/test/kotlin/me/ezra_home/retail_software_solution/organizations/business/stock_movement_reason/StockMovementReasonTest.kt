package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StockMovementReasonTest {

    @Test
    fun `system reasons apply to exactly the movement types in the reason matrix`() {
        val expectedMovementTypesByReason = mapOf(
            StockMovementReason.COUNT_DISCREPANCY to setOf(MovementType.STOCK_ADJUSTMENT_INCREASE, MovementType.STOCK_ADJUSTMENT_DECREASE),
            StockMovementReason.DAMAGED to setOf(MovementType.STOCK_ADJUSTMENT_DECREASE, MovementType.CUSTOMER_RETURN, MovementType.SUPPLIER_RETURN),
            StockMovementReason.QUALITY_ISSUE to setOf(MovementType.STOCK_ADJUSTMENT_DECREASE, MovementType.CUSTOMER_RETURN, MovementType.SUPPLIER_RETURN),
            StockMovementReason.EXPIRED to setOf(MovementType.STOCK_ADJUSTMENT_DECREASE, MovementType.CUSTOMER_RETURN, MovementType.SUPPLIER_RETURN),
            StockMovementReason.THEFT to setOf(MovementType.STOCK_ADJUSTMENT_DECREASE),
            StockMovementReason.INTERNAL_USE to setOf(MovementType.STOCK_ADJUSTMENT_DECREASE),
            StockMovementReason.SURPLUS to setOf(MovementType.STOCK_ADJUSTMENT_DECREASE, MovementType.SUPPLIER_RETURN)
        )

        assertEquals(expectedMovementTypesByReason.keys, StockMovementReason.entries.toSet())
        expectedMovementTypesByReason.forEach { (reason, expectedMovementTypes) ->
            assertEquals(expectedMovementTypes, reason.allowedMovementTypes, reason.name)
        }
    }

    @Test
    fun `system reason codes are unique and fit the code column`() {
        val codes = StockMovementReason.entries.map { it.code }

        assertEquals(codes.size, codes.toSet().size)
        assertEquals(emptyList<String>(), codes.filter { it.length > 20 })
    }

    @Test
    fun `only stock adjustments require a reason`() {
        val reasonRequiredTypes = MovementType.entries.filter { it.reasonRequired }.toSet()

        assertEquals(setOf(MovementType.STOCK_ADJUSTMENT_INCREASE, MovementType.STOCK_ADJUSTMENT_DECREASE), reasonRequiredTypes)
    }

    @Test
    fun `movement type codes are unique and fit the movement type column`() {
        val codes = MovementType.entries.map { it.code }

        assertEquals(codes.size, codes.toSet().size)
        assertEquals(emptyList<String>(), codes.filter { it.length > 5 })
    }
}
