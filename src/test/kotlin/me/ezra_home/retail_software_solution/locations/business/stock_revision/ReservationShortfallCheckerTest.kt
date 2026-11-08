package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock_reservation.api.ProductReservations
import me.ezra_home.retail_software_solution.locations.business.stock_reservation.api.StockReserver
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class ReservationShortfallCheckerTest {

    private val locationProductId = UUID.randomUUID()
    private val firstSaleId = UUID.randomUUID()
    private val secondSaleId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()
    private val quantityBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(BreakdownUnit(unitId, "pc", 1L, unitId, OffsetDateTime.parse("2026-11-01T10:00:00Z"))), unitId
    )
    private val stockBalanceFetcher = mock(StockBalanceFetcher::class.java)
    private val stockReserver = mock(StockReserver::class.java)
    private val reservationShortfallChecker = ReservationShortfallChecker(
        stockBalanceFetcher,
        stockReserver,
        mock(SaleDataFetcher::class.java).also {
            `when`(it.getReferenceNumbersById(setOf(firstSaleId, secondSaleId)))
                .thenReturn(mapOf(firstSaleId to "SAL000420", secondSaleId to "SAL000421"))
        },
        mock(LocationProductDataFetcher::class.java).also {
            val summary = mock(LocationProductSummaryDto::class.java)
            `when`(summary.label).thenReturn("Sugar")
            `when`(it.findSummaryByIds(listOf(locationProductId))).thenReturn(mapOf(locationProductId to summary))
        }
    )

    private fun givenOnHand(onHand: String) {
        `when`(stockBalanceFetcher.getLatestBalances(listOf(locationProductId)))
            .thenReturn(mapOf(locationProductId to BigDecimal(onHand)))
    }

    private fun givenReservedBySale(firstSale: String, secondSale: String) {
        `when`(stockReserver.loadReservationBreakdown(listOf(locationProductId))).thenReturn(
            mapOf(
                locationProductId to ProductReservations(
                    total = BigDecimal(firstSale) + BigDecimal(secondSale),
                    bySale = mapOf(firstSaleId to BigDecimal(firstSale), secondSaleId to BigDecimal(secondSale)),
                )
            )
        )
    }

    @Test
    fun `a product nobody reserved has no problem`() {
        givenOnHand("5")
        `when`(stockReserver.loadReservationBreakdown(listOf(locationProductId))).thenReturn(emptyMap())

        assertTrue(
            reservationShortfallChecker.problemsAfterPendingChange(
                locationProductId, ExactQuantity.ZERO, quantityBreakdown
            ).isEmpty()
        )
    }

    @Test
    fun `reservations the stock still covers have no problem`() {
        givenOnHand("10")
        givenReservedBySale("4", "6")

        assertTrue(
            reservationShortfallChecker.problemsAfterPendingChange(
                locationProductId, ExactQuantity.ZERO, quantityBreakdown
            ).isEmpty()
        )
    }

    @Test
    fun `a pending reduction that leaves reservations short lists every draft sale holding the product`() {
        givenOnHand("10")
        givenReservedBySale("4", "6")

        val problems = reservationShortfallChecker.problemsAfterPendingChange(
            locationProductId, ExactQuantity.ofStorage(BigDecimal("-2")), quantityBreakdown
        )

        assertEquals(listOf("SAL000420 (draft)", "SAL000421 (draft)"), problems.map { it.documentReference })
        assertEquals("Draft sale holding 4 pcs of Sugar.", problems.first().description)
        assertTrue(problems.first().effect.contains("8 pcs"))
        assertTrue(problems.first().effect.contains("10 pcs"))
    }

    @Test
    fun `a pending increase can cover reservations the current stock cannot`() {
        givenOnHand("8")
        givenReservedBySale("4", "6")

        assertTrue(
            reservationShortfallChecker.problemsAfterPendingChange(
                locationProductId, ExactQuantity.ofStorage(BigDecimal("2")), quantityBreakdown
            ).isEmpty()
        )
    }
}
