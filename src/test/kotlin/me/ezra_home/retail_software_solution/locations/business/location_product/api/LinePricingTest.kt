package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

@WithDefaultOrganizationSession
class LinePricingTest {

    private val pricePerBale = BigDecimal("1000.00")

    @Test
    fun `1 bale and 18 pieces of a product priced 1000 per bale both total 1000`() {
        assertEquals(BigDecimal("1000.00"), LinePricing.lineTotal(pricePerBale, BigDecimal("1"), 18L, 18L))
        assertEquals(BigDecimal("1000.00"), LinePricing.lineTotal(pricePerBale, BigDecimal("18"), 1L, 18L))
    }

    @Test
    fun `the unit price derived from the total on the pieces line is 55 point 5556`() {
        val lineTotal = LinePricing.lineTotal(pricePerBale, BigDecimal("18"), 1L, 18L)
        val line = object : ProductLineWithPrice {
            override val locationProductId = UUID.randomUUID()
            override val userProvidedQuantity = BigDecimal("18")
            override val lineTotal = lineTotal
        }

        assertEquals(BigDecimal("55.5556"), line.unitPrice)
    }

    @Test
    fun `a base unit line is price times quantity with no ratio and one rounding`() {
        assertEquals(BigDecimal("30.85"), LinePricing.lineTotal(BigDecimal("12.34"), BigDecimal("2.50"), 1L, 1L))
        assertEquals(BigDecimal("3.33"), LinePricing.lineTotal(BigDecimal("10"), BigDecimal("0.3333"), 1L, 1L))
    }

    @Test
    fun `a fractional bale is priced exactly through the ratio`() {
        assertEquals(BigDecimal("250.00"), LinePricing.lineTotal(pricePerBale, BigDecimal("0.25"), 18L, 18L))
        assertEquals(BigDecimal("250.00"), LinePricing.lineTotal(pricePerBale, BigDecimal("4.5"), 1L, 18L))
    }

    @Test
    fun `a half cent total rounds up to the cent`() {
        assertEquals(BigDecimal("3.13"), LinePricing.lineTotal(BigDecimal("3.125"), BigDecimal("1"), 1L, 1L))
        assertEquals(BigDecimal("0.04"), LinePricing.overrideTotal(BigDecimal("0.15"), BigDecimal("0.25")))
        assertEquals(BigDecimal("15.63"), LinePricing.lineTotal(BigDecimal("150.00"), BigDecimal("5"), 1L, 48L))
    }

    @Test
    fun `an override total multiplies the typed price by the quantity once`() {
        assertEquals(BigDecimal("30.85"), LinePricing.overrideTotal(BigDecimal("12.34"), BigDecimal("2.50")))
    }
}
