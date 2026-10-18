package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

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
    fun `a percentage adjustment with more than two decimals is applied without rounding the rate first`() {
        val calculatedAmount = AdjustmentAmountCalculator.calculateAmount(
            adjustment(PriceAdjustmentMethod.PERCENTAGE, "7.125", onLine = true), listOf(line("1", "1000"))
        )

        assertEquals(BigDecimal("71.2500"), calculatedAmount)
    }

    @Test
    fun `a percentage adjustment takes its base from the stored line total and order level takes the sum of all lines`() {
        val lines = listOf(line("18", "1000.0000"))

        assertEquals(BigDecimal("100.0000"), AdjustmentAmountCalculator.calculateAmount(adjustment(PriceAdjustmentMethod.PERCENTAGE, "10", onLine = true), lines))
        assertEquals(BigDecimal("100.0000"), AdjustmentAmountCalculator.calculateAmount(adjustment(PriceAdjustmentMethod.PERCENTAGE, "10", onLine = false), lines))
    }

    @Test
    fun `a fixed line total is the whole amount and ignores the quantity`() {
        val lines = listOf(line("2.50", "2500.0000"))
        assertEquals(BigDecimal("2469.1500"), AdjustmentAmountCalculator.calculateAmount(adjustment(PriceAdjustmentMethod.FIXED_LINE_TOTAL, "2469.1500", onLine = true), lines))
    }
}
