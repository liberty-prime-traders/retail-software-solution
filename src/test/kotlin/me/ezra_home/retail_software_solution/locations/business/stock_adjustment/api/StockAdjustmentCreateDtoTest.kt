package me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api

import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentCategory
import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.PositiveUserProvidedQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

@WithDefaultOrganizationSession
class StockAdjustmentCreateDtoTest {

    @Test
    fun `the required unit cost is the unit cost when it was supplied`() {
        val unitCost = PositiveMoney.of(BigDecimal("2.50"))

        assertEquals(unitCost, stockAdjustmentCreateDto(unitCost).requiredUnitCost())
    }

    @Test
    fun `a missing unit cost is a business error naming the rule`() {
        val exception = assertThrows(RtsGenericException::class.java) { stockAdjustmentCreateDto(null).requiredUnitCost() }

        assertEquals("Unit cost is required for a stock increase", exception.message)
    }

    private fun stockAdjustmentCreateDto(unitCost: PositiveMoney?) = StockAdjustmentCreateDto(
        locationProductId = UUID.randomUUID(),
        category = StockAdjustmentCategory.FREE_GOODS,
        userProvidedQuantity = PositiveUserProvidedQuantity.of(BigDecimal.ONE),
        unitId = UUID.randomUUID(),
        note = "samples",
        unitCost = unitCost
    )
}
