package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MovementTypeTest {

    @Test
    fun `a reversal increases stock and undoes a movement that decreases it`() {
        assertTrue(MovementType.reversals.all { (reversal, reversedMovement) -> reversal.increasesStock() && reversedMovement.decreasesStock() })
    }

    @Test
    fun `a revision reversal is a reversal that increases stock`() {
        assertTrue(MovementType.REVERSED_BY_REVISION.isReversal())
        assertTrue(MovementType.REVERSED_BY_REVISION.increasesStock())
    }

    @Test
    fun `a movement type that cannot be reversed is not itself a reversal`() {
        assertTrue(MovementType.movementsThatCannotBeReversed.none { it.isReversal() })
    }
}
