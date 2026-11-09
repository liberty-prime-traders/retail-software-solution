package me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api

import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.StockAdjustmentCostEntity
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.StockAdjustmentCostRepository
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.StockAdjustmentEntity
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.StockAdjustmentRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class StockAdjustmentCostRestaterTest {

    private val stockAdjustmentRepository = mock(StockAdjustmentRepository::class.java)
    private val stockAdjustmentCostRepository = mock(StockAdjustmentCostRepository::class.java)
    private val stockAdjustmentCostRestater = StockAdjustmentCostRestater(stockAdjustmentRepository, stockAdjustmentCostRepository)

    private val stockAdjustmentId = UUID.randomUUID()

    @Test
    fun `the active cost row is flipped first and replaced by one that moves by the cost change`() {
        givenAdjustmentWithActiveCost("ADJ1", "100.0000")

        stockAdjustmentCostRestater.restate(listOf(StockAdjustmentCostChange("ADJ1", BigDecimal("75.4000"), BigDecimal("74.1000"))))

        val flippedCaptor = ArgumentCaptor.forClass(StockAdjustmentCostEntity::class.java)
        val insertedCaptor = ArgumentCaptor.forClass(StockAdjustmentCostEntity::class.java)
        verify(stockAdjustmentCostRepository).saveAndFlush(flippedCaptor.capture())
        verify(stockAdjustmentCostRepository).save(insertedCaptor.capture())
        assertEquals(StockAdjustmentCostStatus.REPLACED, flippedCaptor.value.status)
        assertEquals(StockAdjustmentCostStatus.ACTIVE, insertedCaptor.value.status)
        assertEquals(BigDecimal("98.7000"), insertedCaptor.value.totalCost)
        assertEquals(stockAdjustmentId, insertedCaptor.value.stockAdjustmentId)
    }

    @Test
    fun `a change that would leave a negative cost is refused`() {
        givenAdjustmentWithActiveCost("ADJ1", "10.00")

        assertThrows(RtsGenericException::class.java) {
            stockAdjustmentCostRestater.restate(listOf(StockAdjustmentCostChange("ADJ1", BigDecimal("20.00"), BigDecimal("5.00"))))
        }
    }

    @Test
    fun `an adjustment with no cost row is refused`() {
        val stockAdjustment = mock(StockAdjustmentEntity::class.java)
        `when`(stockAdjustment.id).thenReturn(stockAdjustmentId)
        `when`(stockAdjustment.requiredReference()).thenReturn("ADJ1")
        `when`(stockAdjustmentRepository.findByReferenceNumberIn(listOf("ADJ1"))).thenReturn(listOf(stockAdjustment))
        `when`(stockAdjustmentCostRepository.findByStockAdjustmentIdInAndStatus(listOf(stockAdjustmentId), StockAdjustmentCostStatus.ACTIVE))
            .thenReturn(emptyList())

        assertThrows(RtsGenericException::class.java) {
            stockAdjustmentCostRestater.restate(listOf(StockAdjustmentCostChange("ADJ1", BigDecimal.ONE, BigDecimal.TEN)))
        }
    }

    private fun givenAdjustmentWithActiveCost(referenceNumber: String, totalCost: String) {
        val stockAdjustment = mock(StockAdjustmentEntity::class.java)
        `when`(stockAdjustment.id).thenReturn(stockAdjustmentId)
        `when`(stockAdjustment.requiredReference()).thenReturn(referenceNumber)
        `when`(stockAdjustmentRepository.findByReferenceNumberIn(listOf(referenceNumber))).thenReturn(listOf(stockAdjustment))
        `when`(stockAdjustmentCostRepository.findByStockAdjustmentIdInAndStatus(listOf(stockAdjustmentId), StockAdjustmentCostStatus.ACTIVE))
            .thenReturn(listOf(StockAdjustmentCostEntity(stockAdjustmentId = stockAdjustmentId, totalCost = BigDecimal(totalCost))))
    }
}
