package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.FifoLayer
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockEntryFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.StockEntryFifoDto
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityAsEntered
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class ReplayPlannerTest {

    private val stockEntryFetcher = mock(StockEntryFetcher::class.java)
    private val locationProductDataFetcher = mock(LocationProductDataFetcher::class.java)
    private val replayPlanner = ReplayPlanner(stockEntryFetcher, locationProductDataFetcher)

    private val locationProductId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()
    private val replacedStockEntryId = UUID.randomUUID()
    private val quantityBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(BreakdownUnit(unitId, "pcs", 1L, unitId, OffsetDateTime.parse("2026-11-01T10:00:00Z"))), unitId
    )

    private fun givenLayerWithStockLeft(quantity: String, batchCost: String) {
        val fifoLayer = FifoLayer(
            UUID.randomUUID(),
            BigDecimal(quantity),
            BigDecimal(batchCost),
            BigDecimal(quantity),
            BigDecimal(batchCost)
        )
        `when`(stockEntryFetcher.findWithStockLeftInDrawOrder(locationProductId))
            .thenReturn(listOf(StockEntryFifoDto("STE2", locationProductId, BigDecimal.ONE, fifoLayer)))
        `when`(locationProductDataFetcher.findSummaryByIds(anyCollection())).thenReturn(
            mapOf(
                locationProductId to LocationProductSummaryDto(
                    locationProductId,
                    "LP1",
                    UUID.randomUUID(),
                    "Sugar",
                    "Pantry",
                    unitId
                )
            )
        )
    }

    private fun consumption(documentReference: String, quantity: String, cost: String) = StockEntryConsumption(
        stockEntryId = replacedStockEntryId,
        locationProductId = locationProductId,
        consumer = StockConsumer(StockMovementOrigin(documentReference, "SALL1"), MovementType.SALE, null),
        quantityAsEntered = QuantityAsEntered(unitId, 1L, "whole line of $documentReference"),
        orderOfConsumption = OrderOfConsumption(null, null),
        netConsumedStorageQuantity = ExactQuantity.ofStorage(BigDecimal(quantity)),
        netConsumedCost = BigDecimal(cost)
    )

    @Test
    fun `a layer nobody drew from plans nothing and reads nothing`() {
        assertTrue(replayPlanner.plan(emptyList(), "STRV000001", quantityBreakdown).isEmpty())

        verifyNoInteractions(stockEntryFetcher, locationProductDataFetcher)
    }

    @Test
    fun `consumers the later layers can cover are planned at the replacement's cost`() {
        givenLayerWithStockLeft("10", "120.00")

        val consumerReplayPlans = replayPlanner.plan(
            listOf(consumption("SAL1", "4", "40.00")),
            "STRV000001",
            quantityBreakdown
        )

        assertEquals(0, BigDecimal("48.00").compareTo(consumerReplayPlans.single().newCost))
        assertEquals(0, BigDecimal("40.00").compareTo(consumerReplayPlans.single().oldCost))
    }

    @Test
    fun `when any consumer cannot be covered the whole replay is refused with every problem listed`() {
        givenLayerWithStockLeft("5", "60.00")

        val exception = assertThrows(RtsGenericException::class.java) {
            replayPlanner.plan(
                listOf(consumption("SAL1", "4", "40.00"), consumption("SAL2", "4", "40.00")),
                "STRV000001",
                quantityBreakdown
            )
        }

        assertEquals(1, (exception.payload as List<*>).size)
    }
}
