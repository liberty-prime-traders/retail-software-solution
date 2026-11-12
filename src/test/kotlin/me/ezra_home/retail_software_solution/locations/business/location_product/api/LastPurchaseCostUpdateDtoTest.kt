package me.ezra_home.retail_software_solution.locations.business.location_product.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class LastPurchaseCostUpdateDtoTest {

    @Test
    fun `a cost typed for a larger unit is divided down to the base unit`() {
        val baleCostUpdate = LastPurchaseCostUpdateDto(UUID.randomUUID(), BigDecimal("180.00"), 18L)

        assertEquals(BigDecimal("10.0000"), baleCostUpdate.costPerBaseUnit(storageUnitsPerBaseUnit = 1L))
    }

    @Test
    fun `a cost typed in the base unit is unchanged whatever the base unit size`() {
        val baseUnitCostUpdate = LastPurchaseCostUpdateDto(UUID.randomUUID(), BigDecimal("12.50"), 1000L)

        assertEquals(BigDecimal("12.5000"), baseUnitCostUpdate.costPerBaseUnit(storageUnitsPerBaseUnit = 1000L))
    }

    @Test
    fun `a cost per base unit that does not divide evenly rounds half up at four decimals`() {
        val trayCostUpdate = LastPurchaseCostUpdateDto(UUID.randomUUID(), BigDecimal("100.00"), 3L)

        assertEquals(BigDecimal("33.3333"), trayCostUpdate.costPerBaseUnit(storageUnitsPerBaseUnit = 1L))
    }
}
