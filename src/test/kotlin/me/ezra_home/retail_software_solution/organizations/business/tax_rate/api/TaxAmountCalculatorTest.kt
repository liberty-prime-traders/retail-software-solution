package me.ezra_home.retail_software_solution.organizations.business.tax_rate.api

import me.ezra_home.retail_software_solution.organizations.business.tax_rate.api.TaxAmountCalculator.ActiveTaxTypeForCalculation
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxCalculationMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class TaxAmountCalculatorTest {

    @Test
    fun `a percentage rate with more than two decimals is applied without rounding the rate first`() {
        val taxCalculationResult = TaxAmountCalculator.calculate(
            taxableAmount = BigDecimal("1000"),
            activeTaxTypes = listOf(percentageTax("7.125", taxIsIncludedInTaxableAmount = false)),
        )

        assertEquals(BigDecimal("71.25"), taxCalculationResult.entries.single().taxAmount)
    }

    @Test
    fun `an included percentage tax and its base add up to the gross to the cent`() {
        listOf(
            Triple("107.13", "7.125", "100.00" to "7.13"),
            Triple("19.99", "8.25", "18.47" to "1.52"),
            Triple("250.00", "7.5", "232.56" to "17.44"),
            Triple("5.00", "8.875", "4.59" to "0.41"),
        ).forEach { (gross, ratePercentage, baseAndTax) ->
            val taxCalculationEntry = TaxAmountCalculator.calculate(
                taxableAmount = BigDecimal(gross),
                activeTaxTypes = listOf(percentageTax(ratePercentage, taxIsIncludedInTaxableAmount = true)),
            ).entries.single()

            assertEquals(BigDecimal(baseAndTax.first), taxCalculationEntry.resolvedTaxableBase)
            assertEquals(BigDecimal(baseAndTax.second), taxCalculationEntry.taxAmount)
            assertEquals(BigDecimal(gross), taxCalculationEntry.resolvedTaxableBase + taxCalculationEntry.taxAmount)
        }
    }

    @Test
    fun `several included percentage taxes share the tax left after the base and still add up to the gross`() {
        val taxCalculationEntries = TaxAmountCalculator.calculate(
            taxableAmount = BigDecimal("19.99"),
            activeTaxTypes = listOf(
                percentageTax("5", taxIsIncludedInTaxableAmount = true),
                percentageTax("3.25", taxIsIncludedInTaxableAmount = true),
            ),
        ).entries

        assertEquals(BigDecimal("19.99"), taxCalculationEntries.first().resolvedTaxableBase + taxCalculationEntries.sumOf { it.taxAmount })
    }

    private fun percentageTax(ratePercentage: String, taxIsIncludedInTaxableAmount: Boolean) = ActiveTaxTypeForCalculation(
        orgJurisdictionTaxTypeId = UUID.randomUUID(),
        jurisdictionTaxTypeId = UUID.randomUUID(),
        calculationMethod = TaxCalculationMethod.PERCENTAGE,
        activeTaxRateDto = ActiveTaxRateDto(
            ratePercentage = BigDecimal(ratePercentage),
            rateFlatAmount = null,
            taxIsBilledToCustomerSeparately = true,
            taxIsIncludedInTaxableAmount = taxIsIncludedInTaxableAmount,
        ),
    )
}
