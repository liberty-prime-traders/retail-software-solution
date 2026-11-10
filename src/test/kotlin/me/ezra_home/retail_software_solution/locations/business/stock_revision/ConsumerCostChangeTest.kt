package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockMovementDto
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.quantity.QuantityAsEntered
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class ConsumerCostChangeTest {

    private fun movement(movementType: MovementType, documentReference: String, detailReference: String, cost: String) =
        StockMovementDto(
            id = null, createdOn = null, stockEntryId = UUID.randomUUID(), locationProductId = UUID.randomUUID(),
            movementType = movementType, storageMovedQuantity = BigDecimal.ONE, movedCost = BigDecimal(cost),
            documentReference = documentReference, detailReference = detailReference, adjustmentDetails = null,
            quantityAsEntered = QuantityAsEntered(UUID.randomUUID(), 1L, "1")
        )

    @Test
    fun `a revision's movements pair each consumer's restored cost with its replayed cost`() {
        val consumerCostChanges = ConsumerCostChange.ofRevisionMovements(
            listOf(
                movement(MovementType.REVERSED_BY_REVISION, "SAL1", "SALN1", "75.4000"),
                movement(MovementType.SALE, "SAL1", "SALN1", "40.0000"),
                movement(MovementType.SALE, "SAL1", "SALN1", "34.1000"),
                movement(MovementType.REVERSED_BY_REVISION, "ADJ1", "ADJN1", "5.0000"),
                movement(MovementType.STOCK_ADJUSTMENT_DECREASE, "ADJ1", "ADJN1", "4.5000"),
                movement(MovementType.STOCK_REVISION_ZEROING, "DEL1", "DELN1", "9.0000"),
                movement(MovementType.PURCHASE_RECEIVED, "DEL1", "DELN2", "9.0000"),
            )
        )

        assertEquals(2, consumerCostChanges.size)
        val saleCostChange = consumerCostChanges.single { it.movementType == MovementType.SALE }
        assertEquals(BigDecimal("75.4000"), saleCostChange.oldCost)
        assertEquals(BigDecimal("74.1000"), saleCostChange.newCost)
        val adjustmentCostChange = consumerCostChanges.single { it.documentReference == "ADJ1" }
        assertEquals(BigDecimal("5.0000"), adjustmentCostChange.oldCost)
        assertEquals(BigDecimal("4.5000"), adjustmentCostChange.newCost)
    }
}
