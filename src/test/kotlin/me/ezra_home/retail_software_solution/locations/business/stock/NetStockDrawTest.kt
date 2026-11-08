package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class NetStockDrawTest {

    private val locationProductId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()

    @Test
    fun `an unrevised sale nets to each of its draws`() {
        val layerId = UUID.randomUUID()
        val netStockDraws = netSaleDraws(movement(layerId, MovementType.SALE, "4", "32.00"))

        assertEquals(1, netStockDraws.size)
        assertEquals(0, BigDecimal("4").compareTo(netStockDraws.single().storageMovedQuantity))
        assertEquals(0, BigDecimal("32.00").compareTo(netStockDraws.single().movedCost))
    }

    @Test
    fun `a layer whose draw a revision reversed is skipped and the replayed draw is reversed`() {
        val replacedLayerId = UUID.randomUUID()
        val replacementLayerId = UUID.randomUUID()
        val netStockDraws = netSaleDraws(
            movement(replacedLayerId, MovementType.SALE, "4", "32.00"),
            movement(replacedLayerId, MovementType.REVERSED_BY_REVISION, "4", "32.00", "SR000001"),
            movement(replacementLayerId, MovementType.SALE, "4", "36.00", "SR000001"),
        )

        assertEquals(listOf(replacementLayerId), netStockDraws.map { it.stockEntryId })
        assertEquals(0, BigDecimal("36.00").compareTo(netStockDraws.single().movedCost))
    }

    @Test
    fun `a layer drawn twice by the same line gets one reversal for the net`() {
        val layerId = UUID.randomUUID()
        val netStockDraws = netSaleDraws(
            movement(layerId, MovementType.SALE, "3", "24.00"),
            movement(layerId, MovementType.SALE, "1", "8.00", "SR000001"),
        )

        assertEquals(1, netStockDraws.size)
        assertEquals(0, BigDecimal("4").compareTo(netStockDraws.single().storageMovedQuantity))
        assertEquals(0, BigDecimal("32.00").compareTo(netStockDraws.single().movedCost))
    }

    @Test
    fun `a sale revised twice reverses only what still sits on the live layer`() {
        val firstLayerId = UUID.randomUUID()
        val secondLayerId = UUID.randomUUID()
        val thirdLayerId = UUID.randomUUID()
        val netStockDraws = netSaleDraws(
            movement(firstLayerId, MovementType.SALE, "4", "32.00"),
            movement(firstLayerId, MovementType.REVERSED_BY_REVISION, "4", "32.00", "SR000001"),
            movement(secondLayerId, MovementType.SALE, "4", "36.00", "SR000001"),
            movement(secondLayerId, MovementType.REVERSED_BY_REVISION, "4", "36.00", "SR000002"),
            movement(thirdLayerId, MovementType.SALE, "4", "40.00", "SR000002"),
        )

        assertEquals(listOf(thirdLayerId), netStockDraws.map { it.stockEntryId })
    }

    @Test
    fun `a sale already voided nets to nothing`() {
        val layerId = UUID.randomUUID()

        assertEquals(
            emptyList<NetStockDraw>(),
            netSaleDraws(
                movement(layerId, MovementType.SALE, "4", "32.00"),
                movement(layerId, MovementType.SALE_VOID, "4", "32.00"),
            )
        )
    }

    @Test
    fun `two lines drawing from one layer are reversed separately`() {
        val layerId = UUID.randomUUID()
        val netStockDraws = netSaleDraws(
            movement(layerId, MovementType.SALE, "2", "16.00", detailReference = "SALN01"),
            movement(layerId, MovementType.SALE, "3", "24.00", detailReference = "SALN02"),
        )

        assertEquals(setOf("SALN01", "SALN02"), netStockDraws.map { it.detailReference }.toSet())
    }

    private fun netSaleDraws(vararg stockMovementEntities: StockMovementEntity): List<NetStockDraw> =
        NetStockDraw.netOf(stockMovementEntities.toList(), MovementType.SALE)

    private fun movement(
        stockEntryId: UUID,
        movementType: MovementType,
        storageQuantity: String,
        cost: String,
        stockRevisionReference: String? = null,
        detailReference: String = "SALN01",
    ) = StockMovementEntity(
        stockEntryId = stockEntryId,
        locationProductId = locationProductId,
        movementType = movementType,
        storageMovedQuantity = BigDecimal(storageQuantity),
        wholeLineQuantityText = "$storageQuantity Pieces",
        storageRemainingQuantity = BigDecimal.ZERO,
        movedCost = BigDecimal(cost),
        storageMovedBreakdownText = "$storageQuantity Pieces",
        storageRemainingBreakdownText = "0 Pieces",
        documentReference = "SALE-1",
        detailReference = detailReference,
        unitId = unitId,
        storageUnitsPerUserProvidedUnit = 1L,
        stockRevisionReference = stockRevisionReference,
    )
}
