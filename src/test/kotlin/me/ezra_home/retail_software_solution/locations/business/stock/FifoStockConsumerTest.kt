package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.ConversionRatio
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class FifoStockConsumerTest {

    private val locationProductId = UUID.randomUUID()

    @Test
    fun `consume walks layers in priority order and reports cost and ending balance`() {
        val newerLayer = stockEntry(priority = 1, quantity = "10.0000", unitCost = "3.0000")
        val olderLayer = stockEntry(priority = 0, quantity = "4.0000", unitCost = "2.5000")

        val fifoConsumptionResult = FifoStockConsumer.consume(
            fifoConsumptionRequest = request("6.0000"),
            fifoEntries = FifoStockConsumer.groupInFifoOrder(listOf(newerLayer, olderLayer)).getValue(locationProductId),
            startingBalance = BigDecimal("14.0000"),
            productLabel = "Widget",
        )

        assertEquals(BigDecimal("16.0000"), fifoConsumptionResult.cost)
        assertEquals(BigDecimal("8.0000"), fifoConsumptionResult.endingBalance)
        assertEquals(listOf(BigDecimal("10.0000"), BigDecimal("8.0000")), fifoConsumptionResult.movements.map { it.remainingQuantity })
        assertEquals(BigDecimal("0.0000"), olderLayer.quantityRemaining)
        assertEquals(BigDecimal("8.0000"), newerLayer.quantityRemaining)
    }

    @Test
    fun `restore reverses a consumption layer by layer and returns the balance to where it started`() {
        val olderLayer = stockEntry(priority = 0, quantity = "4.0000", unitCost = "2.5000")
        val newerLayer = stockEntry(priority = 1, quantity = "10.0000", unitCost = "3.0000")
        val fifoConsumptionResult = FifoStockConsumer.consume(
            fifoConsumptionRequest = request("6.0000"),
            fifoEntries = listOf(olderLayer, newerLayer),
            startingBalance = BigDecimal("14.0000"),
            productLabel = "Widget",
        )

        val fifoRestorationResult = FifoStockConsumer.restore(
            movementsToReverse = fifoConsumptionResult.movements,
            stockEntriesById = listOf(olderLayer, newerLayer).associateBy { it.id!! },
            startingBalance = fifoConsumptionResult.endingBalance,
            reversalType = MovementType.SALE_VOID,
        )

        assertEquals(BigDecimal("14.0000"), fifoRestorationResult.endingBalance)
        assertEquals(BigDecimal("4.0000"), olderLayer.quantityRemaining)
        assertEquals(BigDecimal("10.0000"), newerLayer.quantityRemaining)
        assertEquals(setOf(MovementType.SALE_VOID), fifoRestorationResult.movements.map { it.movementType }.toSet())
        assertEquals(setOf("SALE-1"), fifoRestorationResult.movements.map { it.externalReferenceNumber }.toSet())
    }

    @Test
    fun `restore undoes a consumption made through a non-identity conversion ratio`() {
        val layer = stockEntry(priority = 0, quantity = "10.0000", unitCost = "2.0000")
        val caseRatio = ConversionRatio(12L, 1L)
        val fifoConsumptionResult = FifoStockConsumer.consume(
            fifoConsumptionRequest = request("6.0000", caseRatio),
            fifoEntries = listOf(layer),
            startingBalance = BigDecimal("10.0000"),
            productLabel = "Widget",
        )

        val fifoRestorationResult = FifoStockConsumer.restore(
            movementsToReverse = fifoConsumptionResult.movements,
            stockEntriesById = mapOf(layer.id!! to layer),
            startingBalance = fifoConsumptionResult.endingBalance,
            reversalType = MovementType.SALE_VOID,
        )

        assertEquals(listOf(BigDecimal("0.5000")), fifoConsumptionResult.movements.map { it.movedQuantity })
        assertEquals(BigDecimal("10.0000"), layer.quantityRemaining)
        assertEquals(BigDecimal("10.0000"), fifoRestorationResult.endingBalance)
    }

    @Test
    fun `consume fails with the product label when layers cannot fill the request`() {
        val onlyLayer = stockEntry(priority = 0, quantity = "2.0000", unitCost = "1.0000")

        val exception = assertThrows(RtsGenericException::class.java) {
            FifoStockConsumer.consume(request("5.0000"), listOf(onlyLayer), BigDecimal("2.0000"), "Widget")
        }

        assertEquals("Insufficient stock for Widget. Available: 2, Requested: 5", exception.message)
    }

    private fun request(baseQuantity: String, conversionRatio: ConversionRatio = ConversionRatio.IDENTITY) = FifoConsumptionRequest(
        locationProductId = locationProductId,
        baseQuantity = BigDecimal(baseQuantity),
        unitId = UUID.randomUUID(),
        conversionRatio = conversionRatio,
        movementType = MovementType.SALE,
        externalReferenceNumber = "SALE-1",
    )

    private fun stockEntry(priority: Int, quantity: String, unitCost: String) = StockEntryEntity(
        locationProductId = locationProductId,
        batchSize = BigDecimal(quantity),
        quantityRemaining = BigDecimal(quantity),
        priority = priority,
        unitCost = BigDecimal(unitCost),
    ).also { it.id = UUID.randomUUID() }
}
