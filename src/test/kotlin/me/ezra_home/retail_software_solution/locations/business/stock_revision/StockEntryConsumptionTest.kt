package me.ezra_home.retail_software_solution.locations.business.stock_revision

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityAsEntered
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class StockEntryConsumptionTest {

    private fun consumptionOn(stockEntryId: UUID, documentReference: String) = StockEntryConsumption(
        stockEntryId = stockEntryId,
        locationProductId = UUID.randomUUID(),
        consumer = StockConsumer(StockMovementOrigin(documentReference, "SALL1"), MovementType.SALE, null),
        quantityAsEntered = QuantityAsEntered(UUID.randomUUID(), 1L, "1"),
        orderOfConsumption = OrderOfConsumption(null, null),
        netConsumedStorageQuantity = ExactQuantity.ofStorage(BigDecimal.ONE),
        netConsumedCost = BigDecimal.TEN
    )

    @Test
    fun `consumptions of different documents on one layer pass`() {
        val stockEntryId = UUID.randomUUID()

        StockEntryConsumption.requireOnOneLayer(
            listOf(consumptionOn(stockEntryId, "SAL1"), consumptionOn(stockEntryId, "SAL2"))
        )
    }

    @Test
    fun `consumptions on two layers are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            StockEntryConsumption.requireOnOneLayer(
                listOf(consumptionOn(UUID.randomUUID(), "SAL1"), consumptionOn(UUID.randomUUID(), "SAL2"))
            )
        }
    }

    @Test
    fun `draws are ordered by creation time then id, with draws that have neither last`() {
        val earlier = OrderOfConsumption(OffsetDateTime.parse("2026-11-01T10:00:00Z"), UUID(0, 2))
        val sameTimeLowerId = OrderOfConsumption(earlier.createdOn, UUID(0, 1))
        val unknown = OrderOfConsumption(null, null)

        assertEquals(listOf(sameTimeLowerId, earlier, unknown), listOf(unknown, earlier, sameTimeLowerId).sorted())
    }
}
