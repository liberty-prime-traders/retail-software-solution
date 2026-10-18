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

        assertEquals(BigDecimal("71.2500"), taxCalculationResult.entries.single().taxAmount)
    }

    @Test
    fun `an included percentage tax backs out of the gross amount exactly`() {
        val taxCalculationResult = TaxAmountCalculator.calculate(
            taxableAmount = BigDecimal("107.125"),
            activeTaxTypes = listOf(percentageTax("7.125", taxIsIncludedInTaxableAmount = true)),
        )

        val taxCalculationEntry = taxCalculationResult.entries.single()
        assertEquals(BigDecimal("100.0000"), taxCalculationEntry.resolvedTaxableBase)
        assertEquals(BigDecimal("7.1250"), taxCalculationEntry.taxAmount)
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
