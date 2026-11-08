package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityAsEntered
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class RestatableConsumersTest {

    private val unitId = UUID.randomUUID()

    private fun consumption(movementType: MovementType) = StockEntryConsumption(
        stockEntryId = UUID.randomUUID(),
        locationProductId = UUID.randomUUID(),
        consumer = StockConsumer(StockMovementOrigin("DOC1", "DOCL1"), movementType, null),
        quantityAsEntered = QuantityAsEntered(unitId, 1L, "whole line"),
        orderOfConsumption = OrderOfConsumption(null, null),
        netConsumedStorageQuantity = ExactQuantity.ofStorage(BigDecimal.ONE),
        netConsumedCost = BigDecimal("10.00"),
    )

    @Test
    fun `sales and adjustment decreases are restatable`() {
        assertDoesNotThrow {
            RestatableConsumers.requireAllRestatable(
                listOf(consumption(MovementType.SALE), consumption(MovementType.STOCK_ADJUSTMENT_DECREASE))
            )
        }
    }

    @Test
    fun `a consumer that cannot be restated is a blocking problem naming its document`() {
        val problems = RestatableConsumers.problemsOf(
            listOf(consumption(MovementType.SALE), consumption(MovementType.TRANSFER_OUT))
        )

        assertEquals(listOf("DOC1"), problems.map { it.documentReference })
        assertEquals(
            "Some of this delivery's stock was taken by this transfer. " +
                "A workflow that can automatically fix this is not yet implemented. Please contact support.",
            problems.single().description
        )
        assertEquals("This line cannot be changed.", problems.single().effect)
    }

    @Test
    fun `any other consumer type is refused and named`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            RestatableConsumers.requireAllRestatable(
                listOf(consumption(MovementType.SALE), consumption(MovementType.TRANSFER_OUT))
            )
        }

        assertTrue(exception.message!!.contains("TRANSFER_OUT"))
    }
}
