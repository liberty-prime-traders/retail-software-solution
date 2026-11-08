package me.ezra_home.retail_software_solution.locations.business.stock_revision

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockEntryDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockEntryFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryStatus
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockMovementDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockMovementFetcher
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.exceptions.BlockedChangeException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityAsEntered
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anySet
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class NetConsumptionCalculatorTest {

    private val stockMovementFetcher = mock(StockMovementFetcher::class.java)
    private val stockEntryFetcher = mock(StockEntryFetcher::class.java)
    private val netConsumptionCalculator = NetConsumptionCalculator(stockMovementFetcher, stockEntryFetcher)

    private val entryReferenceNumber = "STE000001"

    private fun netConsumptions(
        movementsOnEntries: List<StockMovementDto>,
        movementsOnOtherLayers: List<StockMovementDto> = emptyList()
    ): List<StockEntryConsumption> {
        `when`(stockEntryFetcher.get(stockEntryId)).thenReturn(
            StockEntryDto(
                id = stockEntryId,
                referenceNumber = entryReferenceNumber,
                locationProductId = locationProductId,
                sourceType = StockItemSource.PURCHASE,
                documentReference = "DLV1",
                detailReference = "DLL1",
                storageQuantityRemaining = BigDecimal.TEN,
                costRemaining = BigDecimal.TEN,
                status = StockEntryStatus.ACTIVE,
            )
        )
        `when`(stockMovementFetcher.findOnEntry(stockEntryId)).thenReturn(movementsOnEntries)
        `when`(
            stockMovementFetcher.findOfDocuments(
                any(UUID::class.java) ?: locationProductId,
                anySet<String>() ?: emptySet()
            )
        )
            .thenReturn(movementsOnEntries + movementsOnOtherLayers)
        return netConsumptionCalculator.calculateNetConsumptions(stockEntryId)
    }

    private val stockEntryId = UUID.randomUUID()
    private val locationProductId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()
    private val baseTime = OffsetDateTime.parse("2026-11-01T10:00:00Z")

    private fun movement(
        movementType: MovementType,
        quantity: String,
        cost: String,
        documentReference: String?,
        detailReference: String? = "LINE1",
        minutesAfterBase: Long = 0,
        entryId: UUID = stockEntryId,
        drawUnitId: UUID = unitId
    ) = StockMovementDto(
        id = null,
        createdOn = baseTime.plusMinutes(minutesAfterBase),
        stockEntryId = entryId,
        locationProductId = locationProductId,
        movementType = movementType,
        storageMovedQuantity = BigDecimal(quantity),
        movedCost = BigDecimal(cost),
        documentReference = documentReference,
        detailReference = detailReference,
        adjustmentDetails = null,
        quantityAsEntered = QuantityAsEntered(drawUnitId, 1L, "text"),
    )

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

        assertEquals(listOf("LINE1"), netConsumptions.map { it.stockMovementOrigin.detailReference })
    }

    @Test
    fun `a cancelled transfer nets to nothing and a dispatched one remains`() {
        val netConsumptions = netConsumptions(
            listOf(
                received(),
                movement(MovementType.TRANSFER_OUT, "2", "20.00", "DISPATCH1", detailReference = "T1"),
                movement(
                    MovementType.TRANSFER_CANCELLED,
                    "2",
                    "20.00",
                    "DISPATCH1",
                    detailReference = "T1",
                    minutesAfterBase = 5
                ),
                movement(
                    MovementType.TRANSFER_OUT,
                    "3",
                    "30.00",
                    "DISPATCH2",
                    detailReference = "T2",
                    minutesAfterBase = 6
                )
            )
        )

        assertEquals(listOf("DISPATCH2"), netConsumptions.map { it.stockMovementOrigin.documentReference })
    }

    @Test
    fun `the zeroing of a replaced layer is not a consumption`() {
        val netConsumptions = netConsumptions(
            listOf(
                received(),
                movement(MovementType.SALE, "4", "40.00", "SALE1", minutesAfterBase = 1),
                movement(MovementType.REVERSED_BY_REVISION, "4", "40.00", "SALE1", minutesAfterBase = 2),
                movement(MovementType.STOCK_REVISION_ZEROING, "10", "100.00", "DLV1", minutesAfterBase = 3),
            )
        )

        assertTrue(netConsumptions.isEmpty())
    }

    @Test
    fun `two draws of one consumer on a layer are one consumption netted against that consumer's reversals once`() {
        val netConsumptions = netConsumptions(
            listOf(
                received(),
                movement(MovementType.SALE, "4", "40.00", "SALE1", minutesAfterBase = 1),
                movement(MovementType.SALE, "3", "30.00", "SALE1", minutesAfterBase = 2),
                movement(MovementType.SALE_VOID, "2", "20.00", "SALE1", minutesAfterBase = 3),
            )
        )

        val consumption = netConsumptions.single()
        assertEquals(baseTime.plusMinutes(1), consumption.orderOfConsumption.createdOn)
        assertEquals(ExactQuantity.ofStorage(BigDecimal("5")), consumption.netConsumedStorageQuantity)
        assertEquals(BigDecimal("50.00"), consumption.netConsumedCost)
    }

    @Test
    fun `a stock adjustment decrease is a consumption`() {
        val netConsumptions = netConsumptions(
            listOf(
                received(),
                movement(MovementType.STOCK_ADJUSTMENT_DECREASE, "1", "10.00", "ADJ1", detailReference = null)
            )
        )

        assertEquals(MovementType.STOCK_ADJUSTMENT_DECREASE, netConsumptions.single().consumer.movementType)
    }

    @Test
    fun `consumptions come back oldest first whatever order the movements are read in`() {
        val netConsumptions = netConsumptions(
            listOf(
                movement(MovementType.SALE, "3", "30.00", "SALE2", minutesAfterBase = 2),
                movement(MovementType.SALE, "4", "40.00", "SALE1", minutesAfterBase = 1),
                received()
            )
        )

        assertEquals(listOf("SALE1", "SALE2"), netConsumptions.map { it.stockMovementOrigin.documentReference })
    }

    @Test
    fun `a consumer is ordered by its first draw on any layer, not by a replayed draw stamped later on this one`() {
        val earlierLayerId = UUID.randomUUID()
        val netConsumptions = netConsumptions(
            movementsOnEntries = listOf(
                received(),
                movement(MovementType.SALE, "3", "27.00", "SAL3", minutesAfterBase = 2),
                movement(MovementType.SALE, "2", "18.00", "SAL1", minutesAfterBase = 3)
            ),
            movementsOnOtherLayers = listOf(
                movement(MovementType.SALE, "4", "32.00", "SAL1", minutesAfterBase = 1, entryId = earlierLayerId)
            )
        )

        assertEquals(listOf("SAL1", "SAL3"), netConsumptions.map { it.stockMovementOrigin.documentReference })
    }

    @Test
    fun `movements of other products or other documents on the layer do not change a consumer's order`() {
        val netConsumptions = netConsumptions(
            movementsOnEntries = listOf(
                received(),
                movement(MovementType.SALE, "3", "27.00", "SAL3", minutesAfterBase = 2),
                movement(MovementType.SALE, "2", "18.00", "SAL1", minutesAfterBase = 3)
            ),
            movementsOnOtherLayers = listOf(
                movement(MovementType.SALE, "4", "32.00", "SAL2", minutesAfterBase = 1, entryId = UUID.randomUUID())
            )
        )

        assertEquals(listOf("SAL3", "SAL1"), netConsumptions.map { it.stockMovementOrigin.documentReference })
    }

    @Test
    fun `movements that are neither draws nor reversals are ignored`() {
        val netConsumptions = netConsumptions(
            listOf(received(), movement(MovementType.CUSTOMER_RETURN, "1", "10.00", "CR1"))
        )

        assertTrue(netConsumptions.isEmpty())
    }

    @Test
    fun `a reversal with no matching draw on the entry rejects it`() {
        val rejection = assertThrows(BlockedChangeException::class.java) {
            netConsumptions(
                listOf(received(), movement(MovementType.SALE_VOID, "4", "40.00", "SALE1"))
            )
        }

        assertEquals(1, rejection.problems.size)
        assertTrue(rejection.problems.single().effect.contains(entryReferenceNumber))
    }

    @Test
    fun `reversing more than was drawn rejects the entry`() {
        val rejection = assertThrows(BlockedChangeException::class.java) {
            netConsumptions(
                listOf(
                    received(),
                    movement(MovementType.SALE, "4", "40.00", "SALE1"),
                    movement(MovementType.SALE_VOID, "5", "50.00", "SALE1", minutesAfterBase = 5)
                )
            )
        }

        assertEquals(1, rejection.problems.size)
        assertTrue(rejection.problems.single().effect.contains(entryReferenceNumber))
    }

    @Test
    fun `draws of one document in different units are one consumption labelled by its earliest draw`() {
        val laterUnitId = UUID.randomUUID()
        val netConsumption = netConsumptions(
            listOf(
                movement(MovementType.SALE, "3", "30.00", "SALE1", minutesAfterBase = 2, drawUnitId = laterUnitId),
                received(),
                movement(MovementType.SALE, "4", "40.00", "SALE1", minutesAfterBase = 1)
            )
        ).single()

        assertEquals(unitId, netConsumption.quantityAsEntered.unitId)
        assertEquals(ExactQuantity.ofStorage(BigDecimal(7)), netConsumption.netConsumedStorageQuantity)
    }

    @Test
    fun `draws of one document on a layer that differ in type are rejected as inconsistent history`() {
        val rejection = assertThrows(BlockedChangeException::class.java) {
            netConsumptions(
                listOf(
                    received(),
                    movement(MovementType.SALE, "4", "40.00", "DOC1", minutesAfterBase = 1),
                    movement(MovementType.TRANSFER_OUT, "3", "30.00", "DOC1", minutesAfterBase = 2)
                )
            )
        }

        assertEquals(1, rejection.problems.size)
    }

    @Test
    fun `draws of different documents in different units are separate consumptions`() {
        val netConsumptions = netConsumptions(
            listOf(
                received(),
                movement(MovementType.SALE, "4", "40.00", "SALE1", minutesAfterBase = 1),
                movement(MovementType.SALE, "3", "30.00", "SALE2", minutesAfterBase = 2, drawUnitId = UUID.randomUUID())
            )
        )

        assertEquals(listOf("SALE1", "SALE2"), netConsumptions.map { it.stockMovementOrigin.documentReference })
    }

    @Test
    fun `every inconsistency is reported together`() {
        val rejection = assertThrows(BlockedChangeException::class.java) {
            netConsumptions(
                listOf(
                    received(),
                    movement(MovementType.SALE_VOID, "4", "40.00", "SALE1"),
                    movement(MovementType.TRANSFER_CANCELLED, "2", "20.00", "DISPATCH1", detailReference = "T1")
                )
            )
        }

        assertEquals(2, rejection.problems.size)
    }
}
