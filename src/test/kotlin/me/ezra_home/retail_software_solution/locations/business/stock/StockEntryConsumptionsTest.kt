package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyIterable
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class StockEntryConsumptionsTest {

    private val stockMovementRepository = mock(StockMovementRepository::class.java)
    private val stockEntryRepository = mock(StockEntryRepository::class.java)
    private val stockEntryConsumptions = StockEntryConsumptions(stockMovementRepository, stockEntryRepository)

    private val entryReferenceNumber = "STE000001"

    private fun stockEntry(id: UUID, referenceNumber: String) = StockEntryEntity(
        locationProductId = locationProductId,
        storageBatchSize = BigDecimal("10"),
        storageQuantityRemaining = BigDecimal("10"),
        priority = 0,
        unitCost = BigDecimal("10"),
        batchCost = BigDecimal("100.00"),
        costRemaining = BigDecimal("100.00")
    ).also {
        it.id = id
        it.referenceNumber = referenceNumber
    }

    private fun netConsumptions(movementsOnEntries: List<StockMovementEntity>): List<EntryConsumption> {
        val stockEntryIds = movementsOnEntries.map { it.stockEntryId }.toSet()
        `when`(stockEntryRepository.findAllById(anyIterable())).thenReturn(stockEntryIds.map { stockEntry(it, entryReferenceNumber) })
        `when`(stockMovementRepository.findByStockEntryIdInOrderByCreatedOnAscIdAsc(stockEntryIds)).thenReturn(movementsOnEntries)
        return stockEntryConsumptions.netConsumptions(stockEntryIds)
    }

    private val stockEntryId = UUID.randomUUID()
    private val locationProductId = UUID.randomUUID()
    private val baseTime = OffsetDateTime.parse("2026-11-01T10:00:00Z")

    private fun movement(
        movementType: MovementType,
        quantity: String,
        cost: String,
        documentReference: String?,
        detailReference: String? = "LINE1",
        minutesAfterBase: Long = 0,
        reasonId: UUID? = null,
        entryId: UUID = stockEntryId
    ) = StockMovementEntity(
        stockEntryId = entryId,
        locationProductId = locationProductId,
        movementType = movementType,
        reasonId = reasonId,
        storageMovedQuantity = BigDecimal(quantity),
        wholeLineQuantityText = "text",
        storageRemainingQuantity = BigDecimal.ZERO,
        movedCost = BigDecimal(cost),
        storageMovedBreakdownText = "text",
        storageRemainingBreakdownText = "text",
        documentReference = documentReference,
        detailReference = detailReference,
        unitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1L
    ).also { it.createdOn = baseTime.plusMinutes(minutesAfterBase) }

    private fun received() = movement(MovementType.PURCHASE_RECEIVED, "10", "100.00", "DLV1")

    @Test
    fun `an entry nobody drew from has no consumption`() {
        assertTrue(netConsumptions(listOf(received())).isEmpty())
    }

    @Test
    fun `a sale draw is a consumption with its quantity and cost`() {
        val netConsumptions = netConsumptions(
            listOf(received(), movement(MovementType.SALE, "4", "40.00", "SALE1"))
        )

        assertEquals(1, netConsumptions.size)
        assertEquals(ExactQuantity.ofStorage(BigDecimal("4")), netConsumptions.single().netConsumedStorageQuantity)
        assertEquals(BigDecimal("40.00"), netConsumptions.single().netConsumedCost)
    }

    @Test
    fun `a voided sale nets to nothing`() {
        val netConsumptions = netConsumptions(
            listOf(
                received(),
                movement(MovementType.SALE, "4", "40.00", "SALE1"),
                movement(MovementType.SALE_VOID, "4", "40.00", "SALE1", minutesAfterBase = 5)
            )
        )

        assertTrue(netConsumptions.isEmpty())
    }

    @Test
    fun `a void of a different sale line does not cancel the draw`() {
        val netConsumptions = netConsumptions(
            listOf(
                received(),
                movement(MovementType.SALE, "4", "40.00", "SALE1", detailReference = "LINE1"),
                movement(MovementType.SALE, "2", "20.00", "SALE1", detailReference = "LINE2", minutesAfterBase = 1),
                movement(MovementType.SALE_VOID, "2", "20.00", "SALE1", detailReference = "LINE2", minutesAfterBase = 5)
            )
        )

        assertEquals(listOf("LINE1"), netConsumptions.map { it.consumingMovement.detailReference })
    }

    @Test
    fun `a cancelled transfer nets to nothing and a dispatched one remains`() {
        val netConsumptions = netConsumptions(
            listOf(
                received(),
                movement(MovementType.TRANSFER_OUT, "2", "20.00", "DISPATCH1", detailReference = "T1"),
                movement(MovementType.TRANSFER_CANCELLED, "2", "20.00", "DISPATCH1", detailReference = "T1", minutesAfterBase = 5),
                movement(MovementType.TRANSFER_OUT, "3", "30.00", "DISPATCH2", detailReference = "T2", minutesAfterBase = 6)
            )
        )

        assertEquals(listOf("DISPATCH2"), netConsumptions.map { it.consumingMovement.documentReference })
    }

    @Test
    fun `a stock adjustment decrease is a consumption`() {
        val netConsumptions = netConsumptions(
            listOf(received(), movement(MovementType.STOCK_ADJUSTMENT_DECREASE, "1", "10.00", "ADJ1", detailReference = null, reasonId = UUID.randomUUID()))
        )

        assertEquals(MovementType.STOCK_ADJUSTMENT_DECREASE, netConsumptions.single().consumingMovement.movementType)
    }

    @Test
    fun `the same document on two entries is netted per entry`() {
        val otherEntryId = UUID.randomUUID()
        val netConsumptions = netConsumptions(
            listOf(
                movement(MovementType.SALE, "4", "40.00", "SALE1"),
                movement(MovementType.SALE, "3", "30.00", "SALE1", entryId = otherEntryId),
                movement(MovementType.SALE_VOID, "4", "40.00", "SALE1", minutesAfterBase = 5)
            )
        )

        assertEquals(listOf(otherEntryId), netConsumptions.map { it.stockEntryId })
    }

    @Test
    fun `movements that are neither draws nor reversals are ignored`() {
        val netConsumptions = netConsumptions(
            listOf(received(), movement(MovementType.CUSTOMER_RETURN, "1", "10.00", "CR1"))
        )

        assertTrue(netConsumptions.isEmpty())
    }

    @Test
    fun `a reversal with no matching draw on the entries rejects them`() {
        val rejection = assertThrows(RtsGenericException::class.java) {
            netConsumptions(
                listOf(received(), movement(MovementType.SALE_VOID, "4", "40.00", "SALE1"))
            )
        }

        assertEquals(1, (rejection.payload as List<*>).size)
        assertTrue(rejection.message.contains(entryReferenceNumber))
    }

    @Test
    fun `reversing more than was drawn rejects the entries`() {
        val rejection = assertThrows(RtsGenericException::class.java) {
            netConsumptions(
                listOf(
                    received(),
                    movement(MovementType.SALE, "4", "40.00", "SALE1"),
                    movement(MovementType.SALE_VOID, "5", "50.00", "SALE1", minutesAfterBase = 5)
                )
            )
        }

        assertEquals(1, (rejection.payload as List<*>).size)
        assertTrue(rejection.message.contains(entryReferenceNumber))
    }

    @Test
    fun `no entries have no consumption and read nothing`() {
        assertTrue(stockEntryConsumptions.netConsumptions(emptySet()).isEmpty())
    }

    @Test
    fun `every inconsistency is reported together`() {
        val rejection = assertThrows(RtsGenericException::class.java) {
            netConsumptions(
                listOf(
                    received(),
                    movement(MovementType.SALE_VOID, "4", "40.00", "SALE1"),
                    movement(MovementType.TRANSFER_CANCELLED, "2", "20.00", "DISPATCH1", detailReference = "T1")
                )
            )
        }

        assertEquals(2, (rejection.payload as List<*>).size)
    }
}
