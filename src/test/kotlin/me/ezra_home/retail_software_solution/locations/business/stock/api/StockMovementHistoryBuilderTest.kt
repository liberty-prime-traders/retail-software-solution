package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.StockMovementReasonFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraph
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitPlacement
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class StockMovementHistoryBuilderTest {

    private val locationProductId = UUID.randomUUID()
    private val pieceId = UUID.randomUUID()
    private val baleId = UUID.randomUUID()
    private val stockMovementRepository = mock(StockMovementRepository::class.java)
    private val unitValueFetcher = mock(UnitValueFetcher::class.java)
    private val unitConversionGraphFacade = mock(UnitConversionGraphFacade::class.java)
    private val stockMovementReasonFetcher = mock(StockMovementReasonFetcher::class.java)
    private val builder = StockMovementHistoryBuilder(stockMovementRepository, unitValueFetcher, unitConversionGraphFacade, stockMovementReasonFetcher)

    private fun graph(baleSize: Long) = UnitConversionGraph(
        emptyMap(),
        mapOf(pieceId to "Piece", baleId to "Bale"),
        mapOf(pieceId to UnitPlacement(pieceId, 1L), baleId to UnitPlacement(pieceId, baleSize))
    )

    private fun movement(storageUnitsPerUnit: Long, unitId: UUID = baleId, detailReference: String? = "SALN01") = StockMovementEntity(
        stockEntryId = UUID.randomUUID(),
        locationProductId = locationProductId,
        movementType = MovementType.SALE,
        storageMovedQuantity = BigDecimal("10"),
        userProvidedMovedQuantity = BigDecimal("2"),
        storageRemainingQuantity = BigDecimal("26"),
        movedCost = BigDecimal("1.0000"),
        storageMovedBreakdownText = "10 Pieces",
        storageRemainingBreakdownText = "1 Bale 8 Pieces",
        documentReference = "SAL041",
        detailReference = detailReference,
        unitId = unitId,
        storageUnitsPerUnit = storageUnitsPerUnit,
    ).also {
        it.id = UUID.randomUUID()
        it.createdOn = OffsetDateTime.now()
    }

    private fun givenMovements(vararg movements: StockMovementEntity, baleSize: Long = 18L) {
        `when`(stockMovementRepository.findByLocationProductId(locationProductId, Pageable.ofSize(100)))
            .thenReturn(movements.toList())
        `when`(unitValueFetcher.getUnitNamesById()).thenReturn(mapOf(pieceId to "Piece", baleId to "Bale"))
        `when`(unitConversionGraphFacade.getOrLoad()).thenReturn(graph(baleSize))
        `when`(stockMovementReasonFetcher.getNamesById()).thenReturn(emptyMap())
    }

    @Test
    fun `a row shows the stored texts and what the user entered with the joined reference`() {
        givenMovements(movement(18L))

        val row = builder.build(locationProductId).single()

        assertEquals("10 Pieces", row.quantityMoved)
        assertEquals("1 Bale 8 Pieces", row.newQuantity)
        assertEquals("entered 2 Bales on SAL041:SALN01", row.quantityEntered)
        assertEquals("SAL041", row.documentReference)
        assertEquals("SALN01", row.detailReference)
    }

    @Test
    fun `a source with no detail reference shows the document reference alone`() {
        givenMovements(movement(18L, detailReference = null))

        val row = builder.build(locationProductId).single()

        assertEquals("entered 2 Bales on SAL041", row.quantityEntered)
        assertEquals("SAL041", row.documentReference)
        assertEquals(null, row.detailReference)
    }

    @Test
    fun `the ratio-change note appears only when the captured size differs from the current one`() {
        givenMovements(movement(18L), baleSize = 18L)
        assertNull(builder.build(locationProductId).single().conversionDriftNote)

        givenMovements(movement(18L), baleSize = 20L)
        assertEquals("1 Bale was 18 Pieces", builder.build(locationProductId).single().conversionDriftNote)
    }

    @Test
    fun `a deleted unit does not crash the history and shows no ratio note`() {
        val deletedUnitId = UUID.randomUUID()
        givenMovements(movement(18L, unitId = deletedUnitId))

        val row = builder.build(locationProductId).single()

        assertNull(row.conversionDriftNote)
        assertEquals("entered 2 on SAL041:SALN01", row.quantityEntered)
    }

    @Test
    fun `a page of rows loads units, the unit graph and reasons once`() {
        givenMovements(*Array(100) { movement(18L) })

        builder.build(locationProductId)

        verify(unitValueFetcher, times(1)).getUnitNamesById()
        verify(unitConversionGraphFacade, times(1)).getOrLoad()
        verify(stockMovementReasonFetcher, times(1)).getNamesById()
    }
}
