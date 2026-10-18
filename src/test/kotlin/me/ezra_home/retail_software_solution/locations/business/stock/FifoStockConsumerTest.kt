package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class FifoStockConsumerTest {

    private val locationProductId = UUID.randomUUID()
    private val pieceId = UUID.randomUUID()
    private val baleId = UUID.randomUUID()
    private val epoch = OffsetDateTime.parse("2026-01-01T00:00:00Z")
    private val baleBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(
            BreakdownUnit(pieceId, "Piece", 1L, pieceId, epoch),
            BreakdownUnit(baleId, "Bale", 18L, pieceId, epoch.plusMinutes(1)),
        ),
        baleId
    )

    @Test
    fun `consume walks layers in priority order and reports cost and ending balance`() {
        val newerLayer = stockEntry(priority = 1, storageQuantity = "10", batchCost = "30.0000")
        val olderLayer = stockEntry(priority = 0, storageQuantity = "4", batchCost = "10.0000")

        val fifoConsumptionResult = FifoStockConsumer.consume(
            fifoConsumptionRequest = request("6"),
            fifoEntries = FifoStockConsumer.groupInFifoOrder(listOf(newerLayer, olderLayer)).getValue(locationProductId),
            startingBalance = BigDecimal("14.0000"),
            quantityBreakdown = baleBreakdown,
            productLabel = "Widget",
        )

        assertEquals(BigDecimal("16.0000"), fifoConsumptionResult.cost)
        assertEquals(BigDecimal("8.0000"), fifoConsumptionResult.endingBalance)
        assertEquals(listOf(BigDecimal("10.0000"), BigDecimal("8.0000")), fifoConsumptionResult.movements.map { it.storageRemainingQuantity })
        assertEquals(0, BigDecimal("0").compareTo(olderLayer.storageQuantityRemaining))
        assertEquals(0, BigDecimal("8").compareTo(newerLayer.storageQuantityRemaining))
    }

    @Test
    fun `26 pieces sold take exactly 26 from stock and a void restores the layer exactly`() {
        val layer = stockEntry(priority = 0, storageQuantity = "100", batchCost = "200.0000")

        val consumption = FifoStockConsumer.consume(
            request("26", userProvidedQuantity = "26", storageUnitsPerUnit = 1L), listOf(layer), BigDecimal("100"), baleBreakdown, "Bella"
        )

        assertEquals(0, BigDecimal("74").compareTo(layer.storageQuantityRemaining))
        assertEquals(listOf("1 Bale 8 Pieces"), consumption.movements.map { it.storageMovedBreakdownText })

        FifoStockConsumer.restore(consumption.movements, mapOf(layer.id!! to layer), consumption.endingBalance, baleBreakdown, MovementType.SALE_VOID)

        assertEquals(0, BigDecimal("100").compareTo(layer.storageQuantityRemaining))
        assertEquals(0, BigDecimal("200.0000").compareTo(layer.costRemaining))
    }

    @Test
    fun `1 bale sold takes exactly 18 pieces`() {
        val layer = stockEntry(priority = 0, storageQuantity = "100", batchCost = "200.0000")

        FifoStockConsumer.consume(
            request("18", userProvidedQuantity = "1", storageUnitsPerUnit = 18L), listOf(layer), BigDecimal("100"), baleBreakdown, "Bella"
        )

        assertEquals(0, BigDecimal("82").compareTo(layer.storageQuantityRemaining))
    }

    @Test
    fun `1000 bales sold one piece at a time leave exactly zero and no leftover layer`() {
        val layer = stockEntry(priority = 0, storageQuantity = "18000", batchCost = "9000.0000")
        var balance = BigDecimal("18000")
        var totalCost = BigDecimal.ZERO

        repeat(18_000) {
            val consumption = FifoStockConsumer.consume(
                request("1", userProvidedQuantity = "1", storageUnitsPerUnit = 1L), listOf(layer), balance, baleBreakdown, "Bella"
            )
            balance = consumption.endingBalance
            totalCost = totalCost.add(consumption.cost)
        }

        assertEquals(0, BigDecimal.ZERO.compareTo(layer.storageQuantityRemaining))
        assertEquals(0, BigDecimal.ZERO.compareTo(balance))
        assertEquals(0, BigDecimal("9000.0000").compareTo(totalCost))
        assertEquals(0, BigDecimal.ZERO.compareTo(layer.costRemaining))
    }

    @Test
    fun `a stock of 17995 pieces is shown as 999 Bales 13 Pieces in the stored remaining text`() {
        val layer = stockEntry(priority = 0, storageQuantity = "18000", batchCost = "1000.0000")

        val consumption = FifoStockConsumer.consume(
            request("5", userProvidedQuantity = "5", storageUnitsPerUnit = 1L), listOf(layer), BigDecimal("18000"), baleBreakdown, "Bella"
        )

        assertEquals("999 Bales 13 Pieces", consumption.movements.single().storageRemainingBreakdownText)
    }

    @Test
    fun `a two bale sale from two layers shows two rows with the same entered quantity and storage amounts that sum to 36`() {
        val firstLayer = stockEntry(priority = 0, storageQuantity = "10", batchCost = "10.0000")
        val secondLayer = stockEntry(priority = 1, storageQuantity = "26", batchCost = "52.0000")

        val consumption = FifoStockConsumer.consume(
            request("36", userProvidedQuantity = "2", storageUnitsPerUnit = 18L, detailReference = "SALN01"),
            listOf(firstLayer, secondLayer), BigDecimal("36"), baleBreakdown, "Bella"
        )

        assertEquals(listOf("10 Pieces", "1 Bale 8 Pieces"), consumption.movements.map { it.storageMovedBreakdownText })
        assertEquals(listOf(BigDecimal("2"), BigDecimal("2")), consumption.movements.map { it.userProvidedMovedQuantity })
        assertEquals(setOf(18L), consumption.movements.map { it.storageUnitsPerUnit }.toSet())
        assertEquals(0, BigDecimal("36").compareTo(consumption.movements.sumOf { it.storageMovedQuantity }))
        assertEquals(setOf("SALN01"), consumption.movements.map { it.detailReference }.toSet())
        assertEquals(listOf("1 Bale 8 Pieces", "0 Bales"), consumption.movements.map { it.storageRemainingBreakdownText })
    }

    @Test
    fun `a void's reversal rows carry the original references and entered quantity and render their own texts`() {
        val firstLayer = stockEntry(priority = 0, storageQuantity = "10", batchCost = "10.0000")
        val secondLayer = stockEntry(priority = 1, storageQuantity = "26", batchCost = "52.0000")
        val consumption = FifoStockConsumer.consume(
            request("36", userProvidedQuantity = "2", storageUnitsPerUnit = 18L, detailReference = "SALN01"),
            listOf(firstLayer, secondLayer), BigDecimal("36"), baleBreakdown, "Bella"
        )

        val restoration = FifoStockConsumer.restore(
            consumption.movements, listOf(firstLayer, secondLayer).associateBy { it.id!! }, consumption.endingBalance,
            baleBreakdown, MovementType.SALE_VOID
        )

        assertEquals(setOf("SALE-1"), restoration.movements.map { it.documentReference }.toSet())
        assertEquals(setOf("SALN01"), restoration.movements.map { it.detailReference }.toSet())
        assertEquals(setOf(BigDecimal("2")), restoration.movements.map { it.userProvidedMovedQuantity }.toSet())
        assertEquals(listOf("10 Pieces", "1 Bale 8 Pieces"), restoration.movements.map { it.storageMovedBreakdownText })
        assertEquals(listOf("10 Pieces", "2 Bales"), restoration.movements.map { it.storageRemainingBreakdownText })
    }

    @Test
    fun `a layer of 3000000 grams costing 100 consumed in parts has takes that sum to exactly 100 and a void restores the cost exactly`() {
        val layer = stockEntry(priority = 0, storageQuantity = "3000000", batchCost = "100.0000")
        var balance = BigDecimal("3000000")
        val movements = mutableListOf<StockMovementEntity>()
        listOf("1", "999999", "7", "1000000", "999993").forEach { grams ->
            val consumption = FifoStockConsumer.consume(
                request(grams, userProvidedQuantity = grams, storageUnitsPerUnit = 1L), listOf(layer), balance, baleBreakdown, "Salt"
            )
            balance = consumption.endingBalance
            movements.addAll(consumption.movements)
        }

        assertEquals(0, BigDecimal("100.0000").compareTo(movements.sumOf { it.movedCost }))
        assertEquals(0, BigDecimal.ZERO.compareTo(layer.costRemaining))

        FifoStockConsumer.restore(movements, mapOf(layer.id!! to layer), balance, baleBreakdown, MovementType.SALE_VOID)

        assertEquals(0, BigDecimal("100.0000").compareTo(layer.costRemaining))
        assertEquals(0, BigDecimal("3000000").compareTo(layer.storageQuantityRemaining))
    }

    @Test
    fun `a million pieces at a cost per bale that does not divide by 18 still sums to its batch cost`() {
        val layer = stockEntry(priority = 0, storageQuantity = "1000000", batchCost = "55555.5500")
        var balance = BigDecimal("1000000")
        val movedCosts = mutableListOf<BigDecimal>()
        repeat(7) {
            val consumption = FifoStockConsumer.consume(
                request("142857", userProvidedQuantity = "142857", storageUnitsPerUnit = 1L), listOf(layer), balance, baleBreakdown, "Bella"
            )
            balance = consumption.endingBalance
            movedCosts.add(consumption.cost)
        }
        val lastConsumption = FifoStockConsumer.consume(
            request("1", userProvidedQuantity = "1", storageUnitsPerUnit = 1L), listOf(layer), balance, baleBreakdown, "Bella"
        )
        movedCosts.add(lastConsumption.cost)

        assertEquals(0, BigDecimal("55555.5500").compareTo(movedCosts.fold(BigDecimal.ZERO, BigDecimal::add)))
    }

    @Test
    fun `a sale consuming three layers at different costs has a cost equal to the sum of its movements`() {
        val layers = listOf(
            stockEntry(priority = 0, storageQuantity = "3", batchCost = "10.0001"),
            stockEntry(priority = 1, storageQuantity = "7", batchCost = "33.3333"),
            stockEntry(priority = 2, storageQuantity = "11", batchCost = "99.9999"),
        )

        val consumption = FifoStockConsumer.consume(
            request("15", userProvidedQuantity = "15", storageUnitsPerUnit = 1L), layers, BigDecimal("21"), baleBreakdown, "Widget"
        )

        assertEquals(3, consumption.movements.size)
        assertEquals(0, consumption.cost.compareTo(consumption.movements.sumOf { it.movedCost }))
    }

    @Test
    fun `sufficiency is exact so one storage unit more than available is rejected with breakdown figures`() {
        val onlyLayer = stockEntry(priority = 0, storageQuantity = "35", batchCost = "70.0000")

        val exception = assertThrows(RtsGenericException::class.java) {
            FifoStockConsumer.consume(
                request("36", userProvidedQuantity = "2", storageUnitsPerUnit = 18L), listOf(onlyLayer), BigDecimal("35"), baleBreakdown, "Bella"
            )
        }

        assertEquals("Insufficient stock for Bella. Available: 1 Bale 17 Pieces, Requested: 2 Bales", exception.message)
    }

    @Test
    fun `a fractional root quantity and a fractional bale convert exactly`() {
        val layer = stockEntry(priority = 0, storageQuantity = "20", batchCost = "40.0000")

        val consumption = FifoStockConsumer.consume(
            request("4.5", userProvidedQuantity = "0.25", storageUnitsPerUnit = 18L), listOf(layer), BigDecimal("20"), baleBreakdown, "Bella"
        )

        assertEquals(0, BigDecimal("4.5").compareTo(consumption.movements.single().storageMovedQuantity))
        assertEquals(0, BigDecimal("15.5").compareTo(layer.storageQuantityRemaining))
        assertEquals("4.5 Pieces", consumption.movements.single().storageMovedBreakdownText)
    }

    @Test
    fun `a stock quantity round-trips at four decimals`() {
        val quantity = ExactQuantity.ofUserProvided(BigDecimal("0.0001"), 1L)

        assertEquals(BigDecimal("0.0001"), quantity.toStorageQuantity())
    }

    private fun request(
        storageQuantity: String,
        userProvidedQuantity: String = storageQuantity,
        storageUnitsPerUnit: Long = 1L,
        detailReference: String? = "SALN01",
    ) = FifoConsumptionRequest(
        locationProductId = locationProductId,
        storageQuantity = ExactQuantity.ofStorage(BigDecimal(storageQuantity)),
        userProvidedQuantity = BigDecimal(userProvidedQuantity),
        unitId = UUID.randomUUID(),
        storageUnitsPerUnit = storageUnitsPerUnit,
        movementType = MovementType.SALE,
        documentReference = "SALE-1",
        detailReference = detailReference,
    )

    private fun stockEntry(priority: Int, storageQuantity: String, batchCost: String) = StockEntryEntity(
        locationProductId = locationProductId,
        storageBatchSize = BigDecimal(storageQuantity),
        storageQuantityRemaining = BigDecimal(storageQuantity),
        priority = priority,
        unitCost = BigDecimal.ONE,
        batchCost = BigDecimal(batchCost),
        costRemaining = BigDecimal(batchCost),
    ).also { it.id = UUID.randomUUID() }
}
