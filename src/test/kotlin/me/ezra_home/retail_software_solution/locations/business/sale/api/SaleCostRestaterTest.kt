package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineCogsEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineCogsRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class SaleCostRestaterTest {

    private val saleLineRepository = mock(SaleLineRepository::class.java)
    private val saleLineCogsRepository = mock(SaleLineCogsRepository::class.java)
    private val saleCostRestater = SaleCostRestater(saleLineRepository, saleLineCogsRepository)

    private val saleLineId = UUID.randomUUID()

    @Test
    fun `the active cost row is replaced by one that moves by the revised layer's cost change`() {
        val saleLine = mock(SaleLineEntity::class.java)
        `when`(saleLine.id).thenReturn(saleLineId)
        `when`(saleLine.requiredReference()).thenReturn("SALN1")
        `when`(saleLineRepository.findByReferenceNumberIn(listOf("SALN1"))).thenReturn(listOf(saleLine))
        val activeCogs = SaleLineCogsEntity(saleLineId = saleLineId, cogsTotal = BigDecimal("100.0000"))
        `when`(saleLineCogsRepository.findBySaleLineIdInAndStatus(listOf(saleLineId), SaleLineCogsStatus.ACTIVE))
            .thenReturn(listOf(activeCogs))

        saleCostRestater.restate(listOf(SaleLineCostChange("SALN1", BigDecimal("75.4000"), BigDecimal("74.1000"))))

        val savedCaptor = ArgumentCaptor.forClass(SaleLineCogsEntity::class.java)
        verify(saleLineCogsRepository, times(2)).save(savedCaptor.capture())
        assertEquals(SaleLineCogsStatus.REPLACED, savedCaptor.allValues[0].status)
        assertEquals(SaleLineCogsStatus.ACTIVE, savedCaptor.allValues[1].status)
        assertEquals(BigDecimal("98.7000"), savedCaptor.allValues[1].cogsTotal)
    }

    @Test
    fun `a change that would leave a negative cost is refused`() {
        val saleLine = mock(SaleLineEntity::class.java)
        `when`(saleLine.id).thenReturn(saleLineId)
        `when`(saleLine.requiredReference()).thenReturn("SALN1")
        `when`(saleLineRepository.findByReferenceNumberIn(listOf("SALN1"))).thenReturn(listOf(saleLine))
        `when`(saleLineCogsRepository.findBySaleLineIdInAndStatus(listOf(saleLineId), SaleLineCogsStatus.ACTIVE))
            .thenReturn(listOf(SaleLineCogsEntity(saleLineId = saleLineId, cogsTotal = BigDecimal("10.00"))))

        assertThrows(RtsGenericException::class.java) {
            saleCostRestater.restate(listOf(SaleLineCostChange("SALN1", BigDecimal("20.00"), BigDecimal("5.00"))))
        }
    }
}
