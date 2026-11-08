package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StockConsumerLabelTest {

    @Test
    fun `every type that can consume stock has a shop name`() {
        val labels = MovementType.entries
            .filter { it.isConsumption() }
            .associateWith { StockConsumerLabel.of(it) }

        assertEquals(
            mapOf(
                MovementType.SALE to "Sale",
                MovementType.SUPPLIER_RETURN to "Return to supplier",
                MovementType.STOCK_ADJUSTMENT_DECREASE to "Stock adjustment",
                MovementType.TRANSFER_OUT to "Transfer",
            ),
            labels
        )
    }
}
