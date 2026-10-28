package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

@WithDefaultOrganizationSession
class AdjustmentAmountCalculatorTest {

    private val locationProductId = UUID.randomUUID()

    private fun line(userProvidedQuantity: String, lineTotal: String) = object : ProductLineWithPrice {
        override val locationProductId: UUID = this@AdjustmentAmountCalculatorTest.locationProductId
        override val userProvidedQuantity: BigDecimal = BigDecimal(userProvidedQuantity)
        override val lineTotal: BigDecimal = BigDecimal(lineTotal)
    }

    private fun adjustment(method: PriceAdjustmentMethod, value: String, onLine: Boolean) = SaleAdjustmentCreateDto(
        locationProductId = if (onLine) locationProductId else null,
        direction = AdjustmentDirection.DISCOUNT,
        calculationMethod = method,
        value = BigDecimal(value),
        adjustmentReasonId = UUID.randomUUID(),
    )

    @Test
    fun `a percentage adjustment takes its base from the stored line total and order level takes the sum of all lines`() {
        val lines = listOf(line("18", "1000.0000"))

        assertEquals(BigDecimal("100.00"), AdjustmentAmountCalculator.calculateAmount(adjustment(PriceAdjustmentMethod.PERCENTAGE, "10", onLine = true), lines))
        assertEquals(BigDecimal("100.00"), AdjustmentAmountCalculator.calculateAmount(adjustment(PriceAdjustmentMethod.PERCENTAGE, "10", onLine = false), lines))
    }

    @Test
    fun `a percentage adjustment landing on a half cent rounds up to the cent`() {
        val calculatedAmount = AdjustmentAmountCalculator.calculateAmount(
            adjustment(PriceAdjustmentMethod.PERCENTAGE, "2.5", onLine = true), listOf(line("1", "0.50"))
        )

        assertEquals(BigDecimal("0.01"), calculatedAmount)
    }

    @Test
    fun `a unit price override is the line total minus the typed price times the quantity`() {
        val lines = listOf(line("2.50", "2500.0000"))
        assertEquals(BigDecimal("2469.15"), AdjustmentAmountCalculator.calculateAmount(adjustment(PriceAdjustmentMethod.UNIT_PRICE_OVERRIDE, "12.34", onLine = true), lines))
    }

    @Test
    fun `a unit price override on a sub-cent product follows the typed price, not a rounded unit price`() {
        val lines = listOf(line("0.25", "250.0000"))
        assertEquals(BigDecimal("249.96"), AdjustmentAmountCalculator.calculateAmount(adjustment(PriceAdjustmentMethod.UNIT_PRICE_OVERRIDE, "0.15", onLine = true), lines))
        val afterQuantityEdit = listOf(line("4", "4000.0000"))
        assertEquals(BigDecimal("3999.40"), AdjustmentAmountCalculator.calculateAmount(adjustment(PriceAdjustmentMethod.UNIT_PRICE_OVERRIDE, "0.15", onLine = true), afterQuantityEdit))
    }
}
