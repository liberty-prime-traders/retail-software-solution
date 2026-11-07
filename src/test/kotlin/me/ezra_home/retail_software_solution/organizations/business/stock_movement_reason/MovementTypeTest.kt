package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.StockDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// The sets are spelled out so a new MovementType fails here until someone decides what it is to net consumption,
// restore and replay.
class MovementTypeTest {

    @Test
    fun `the types that draw stock for a consumer are exactly these`() {
        assertEquals(
            setOf(
                MovementType.SALE,
                MovementType.SUPPLIER_RETURN,
                MovementType.STOCK_ADJUSTMENT_DECREASE,
                MovementType.TRANSFER_OUT
            ),
            MovementType.entries.filter { it.isConsumption() }.toSet()
        )
    }

    @Test
    fun `the types that undo a draw are exactly these, and each puts stock back`() {
        val reversals = MovementType.entries.filter { it.isReversal() }.toSet()

        assertEquals(
            setOf(MovementType.SALE_VOID, MovementType.TRANSFER_CANCELLED, MovementType.REVERSED_BY_REVISION),
            reversals
        )
        assertTrue(reversals.all { it.stockDirection == StockDirection.INCREASE })
    }

    @Test
    fun `zeroing a replaced layer decreases stock without being a consumption or a reversal`() {
        val zeroing = MovementType.STOCK_REVISION_ZEROING

        assertTrue(zeroing.decreasesStock())
        assertFalse(zeroing.isConsumption())
        assertFalse(zeroing.isReversal())
    }

    @Test
    fun `every type that puts stock in is a reversal, a layer creator, or one that cannot be reversed`() {
        val layerCreators = setOf(
            MovementType.PURCHASE_RECEIVED,
            MovementType.OPENING_STOCK,
            MovementType.TRANSFER_IN,
            MovementType.STOCK_ADJUSTMENT_INCREASE
        )
        val increasing = MovementType.entries.filter { it.stockDirection == StockDirection.INCREASE }.toSet()
        val reversals = MovementType.entries.filter { it.isReversal() }.toSet()
        val increasingAndCannotBeReversed =
            MovementType.movementsThatCannotBeReversed.filter { it.stockDirection == StockDirection.INCREASE }

        assertEquals(increasing, reversals + layerCreators + increasingAndCannotBeReversed)
        assertTrue(layerCreators.none { it.isReversal() })
    }

    @Test
    fun `no type is both a consumption and a reversal`() {
        assertTrue(MovementType.entries.none { it.isConsumption() && it.isReversal() })
    }

    @Test
    fun `a type that cannot be reversed is never a reversal`() {
        assertTrue(MovementType.movementsThatCannotBeReversed.none { it.isReversal() })
    }
}
