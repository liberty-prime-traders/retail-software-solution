package me.ezra_home.retail_software_solution.locations.business.stock.api.revision

import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryStatus
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.FifoConsumptionRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.PlannedTake
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockBalanceFetcher
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyIterable
import org.mockito.Mockito.`when`
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class StockRevisionCommitterTest {

    private val entityAdvisoryLock = mock(EntityAdvisoryLock::class.java)
    private val stockEntryRepository = mock(StockEntryRepository::class.java)
    private val stockMovementRepository = mock(StockMovementRepository::class.java)
    private val stockBalanceFetcher = mock(StockBalanceFetcher::class.java)
    private val stockRevisionCommitter = StockRevisionCommitter(
        entityAdvisoryLock, stockEntryRepository, stockMovementRepository, stockBalanceFetcher
    )

    private val stockEntryId = UUID.randomUUID()
    private val locationProductId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()
    private val quantityBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(BreakdownUnit(unitId, "pcs", 1L, unitId, OffsetDateTime.now())),
        unitId
    )
    private val savedMovements = mutableListOf<StockMovementEntity>()

    private fun captureSavedMovements() {
        `when`(stockMovementRepository.saveAll(anyIterable<StockMovementEntity>())).thenAnswer { invocation ->
            val movements = invocation.getArgument<Iterable<StockMovementEntity>>(0).toList()
            savedMovements.addAll(movements)
            movements
        }
    }

    private fun stockEntry(status: StockEntryStatus = StockEntryStatus.ACTIVE) = StockEntryEntity(
        locationProductId = locationProductId,
        storageBatchSize = BigDecimal("10"),
        storageQuantityRemaining = BigDecimal("6"),
        priority = 0,
        unitCost = BigDecimal("10"),
        batchCost = BigDecimal("100.00"),
        costRemaining = BigDecimal("60.00"),
        status = status
    ).also {
        it.id = stockEntryId
        it.referenceNumber = "STE000001"
    }

    private fun draft(movementType: MovementType, quantity: String, balanceAfter: String) = StockMovementDraft(
        movementType = movementType,
        storageMoved = ExactQuantity.ofStorage(BigDecimal(quantity)),
        balanceAfterMovement = ExactQuantity.ofStorage(BigDecimal(balanceAfter)),
        movedCost = BigDecimal("10.00"),
        wholeLineQuantityText = "text",
        documentReference = "DOC1",
        detailReference = "LINE1",
        unitId = unitId,
        storageUnitsPerUserProvidedUnit = 1L,
        context = "context",
        stockRevisionReference = "STRV000001",
    )

    private fun commit(stockEntry: StockEntryEntity, drafts: List<StockMovementDraft>) {
        `when`(stockEntryRepository.getReferenceById(stockEntryId)).thenReturn(stockEntry)
        captureSavedMovements()
        stockRevisionCommitter.commitReset(
            StockEntryResetPlan(stockEntryId, locationProductId, drafts),
            quantityBreakdown
        )
    }

    @Test
    fun `a reset empties the entry, marks it replaced and writes the planned movements`() {
        val stockEntry = stockEntry()

        commit(
            stockEntry,
            listOf(
                draft(MovementType.REVERSED_BY_REVISION, "4", "10"),
                draft(MovementType.STOCK_REVISION_ZEROING, "10", "0")
            )
        )

        assertEquals(0, BigDecimal.ZERO.compareTo(stockEntry.storageQuantityRemaining))
        assertEquals(0, BigDecimal.ZERO.compareTo(stockEntry.costRemaining))
        assertEquals(StockEntryStatus.REPLACED, stockEntry.status)
        assertEquals(
            listOf(MovementType.REVERSED_BY_REVISION, MovementType.STOCK_REVISION_ZEROING),
            savedMovements.map { it.movementType }
        )
        assertEquals(stockEntryId, savedMovements.first().stockEntryId)
        assertEquals("STRV000001", savedMovements.first().stockRevisionReference)
        assertEquals("context", savedMovements.first().context)
    }

    @Test
    fun `an entry with nothing planned is still marked replaced`() {
        val stockEntry = stockEntry()

        commit(stockEntry, emptyList())

        assertEquals(StockEntryStatus.REPLACED, stockEntry.status)
        assertTrue(savedMovements.isEmpty())
    }

    @Test
    fun `the product lock is taken before the entry is read`() {
        commit(stockEntry(), emptyList())

        val lockThenRead = inOrder(entityAdvisoryLock, stockEntryRepository)
        lockThenRead.verify(entityAdvisoryLock).acquire(LockNamespaces.PRODUCT, locationProductId)
        lockThenRead.verify(stockEntryRepository).getReferenceById(stockEntryId)
    }

    private fun layer(
        reference: String,
        quantity: String,
        batchCost: String,
        status: StockEntryStatus = StockEntryStatus.ACTIVE,
        entryLocationProductId: UUID = locationProductId
    ) = StockEntryEntity(
        locationProductId = entryLocationProductId,
        storageBatchSize = BigDecimal(quantity),
        storageQuantityRemaining = BigDecimal(quantity),
        priority = 0,
        unitCost = BigDecimal.ONE,
        batchCost = BigDecimal(batchCost),
        costRemaining = BigDecimal(batchCost),
        status = status,
    ).also {
        it.id = UUID.randomUUID()
        it.referenceNumber = reference
    }

    private fun replayPlan(
        documentReference: String,
        takeFrom: StockEntryEntity,
        quantity: String,
        takenCost: String,
        oldCost: String
    ) = ConsumerReplayPlan(
        fifoConsumptionRequest = FifoConsumptionRequest(
            locationProductId = locationProductId,
            storageQuantity = ExactQuantity.ofStorage(BigDecimal(quantity)),
            userProvidedQuantity = BigDecimal(quantity),
            unitId = unitId,
            storageUnitsPerUserProvidedUnit = 1L,
            movementType = MovementType.SALE,
            documentReference = documentReference,
            detailReference = "SALL1",
            wholeLineQuantityText = "whole line of $documentReference",
            stockRevisionReference = "STRV000001",
        ),
        plannedTakes = listOf(
            PlannedTake(takeFrom.id!!, ExactQuantity.ofStorage(BigDecimal(quantity)), BigDecimal(takenCost))
        ),
        oldCost = BigDecimal(oldCost),
    )

    private fun givenLayersWithStockLeft(layers: List<StockEntryEntity>) {
        `when`(stockEntryRepository.findFifoEntriesForProducts(anyCollection())).thenReturn(layers)
        `when`(stockBalanceFetcher.getLatestBalances(anyCollection())).thenReturn(
            mapOf(locationProductId to layers.sumOf { it.storageQuantityRemaining })
        )
        captureSavedMovements()
    }

    private fun givenSavedReplacement(replacement: StockEntryEntity) {
        `when`(stockEntryRepository.findById(replacement.id!!)).thenReturn(Optional.of(replacement))
    }

    @Test
    fun `a replay draws the planned layers down and saves the movements at the replayed cost`() {
        val replacement = layer("STE2", "10", "120.00")
        givenLayersWithStockLeft(listOf(replacement))
        givenSavedReplacement(replacement)

        stockRevisionCommitter.commitReplay(
            listOf(replayPlan("SAL1", replacement, "4", "48.00", "40.00")), replacement.id!!, quantityBreakdown
        )

        assertEquals(BigDecimal("6.0000"), replacement.storageQuantityRemaining)
        assertEquals(BigDecimal("72.00"), replacement.costRemaining)
        assertEquals(BigDecimal("48.00"), savedMovements.single().movedCost)
        assertEquals(listOf(replacement.id), savedMovements.map { it.stockEntryId })
    }

    @Test
    fun `a replay of nothing writes nothing and takes no lock`() {
        stockRevisionCommitter.commitReplay(emptyList(), UUID.randomUUID(), quantityBreakdown)

        verify(entityAdvisoryLock, never()).acquire(LockNamespaces.PRODUCT, locationProductId)
        verify(stockMovementRepository, never()).saveAll(anyIterable<StockMovementEntity>())
    }

    @Test
    fun `a replacement that was never saved is refused even when later layers could cover the consumer`() {
        val later = layer("STE3", "10", "90.00")
        givenLayersWithStockLeft(listOf(later))

        assertThrows(IllegalArgumentException::class.java) {
            stockRevisionCommitter.commitReplay(
                listOf(replayPlan("SAL1", later, "4", "36.00", "40.00")),
                UUID.randomUUID(),
                quantityBreakdown
            )
        }

        assertEquals(BigDecimal("10"), later.storageQuantityRemaining)
        verify(stockMovementRepository, never()).saveAll(anyIterable<StockMovementEntity>())
    }

    @Test
    fun `a replacement that is already replaced or belongs to another product is refused`() {
        val later = layer("STE3", "10", "90.00")
        givenLayersWithStockLeft(listOf(later))
        val alreadyReplaced = layer("STE2", "0", "0.00", status = StockEntryStatus.REPLACED)
        val ofAnotherProduct = layer("STE4", "0", "0.00", entryLocationProductId = UUID.randomUUID())
        givenSavedReplacement(alreadyReplaced)
        givenSavedReplacement(ofAnotherProduct)
        val plans = listOf(replayPlan("SAL1", later, "4", "36.00", "40.00"))

        assertThrows(IllegalArgumentException::class.java) { stockRevisionCommitter.commitReplay(
            plans,
            alreadyReplaced.id!!,
            quantityBreakdown
        ) }
        assertThrows(IllegalArgumentException::class.java) { stockRevisionCommitter.commitReplay(
            plans,
            ofAnotherProduct.id!!,
            quantityBreakdown
        ) }

        verify(stockMovementRepository, never()).saveAll(anyIterable<StockMovementEntity>())
    }

    @Test
    fun `a zero quantity replacement is accepted and later layers cover the consumer`() {
        val zeroReplacement = layer("STE2", "0", "0.00")
        val later = layer("STE3", "10", "90.00")
        givenLayersWithStockLeft(listOf(later))
        givenSavedReplacement(zeroReplacement)

        stockRevisionCommitter.commitReplay(
            listOf(replayPlan("SAL1", later, "4", "36.00", "40.00")), zeroReplacement.id!!, quantityBreakdown
        )

        assertEquals(listOf(later.id), savedMovements.map { it.stockEntryId })
        assertEquals(BigDecimal("36.00"), savedMovements.single().movedCost)
    }
}
