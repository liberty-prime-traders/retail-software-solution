package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@WithDefaultOrganizationSession
class LinePricingTest {

    @Test
    fun `a half cent override total rounds up to the cent`() {
        assertEquals(BigDecimal("0.04"), LinePricing.overrideTotal(BigDecimal("0.15"), BigDecimal("0.25")))
    }

    @Test
    fun `an override total multiplies the typed price by the quantity once`() {
        assertEquals(BigDecimal("30.85"), LinePricing.overrideTotal(BigDecimal("12.34"), BigDecimal("2.50")))
    }

    @Test
    fun `the override difference is positive for a lower override and negative for a higher one`() {
        val lineTotal = BigDecimal("40.00")

        assertEquals(BigDecimal("9.15"), LinePricing.overrideDifference(lineTotal, BigDecimal("12.34"), BigDecimal("2.50")))
        assertEquals(BigDecimal("-10.00"), LinePricing.overrideDifference(lineTotal, BigDecimal("20.00"), BigDecimal("2.50")))
    }
}
