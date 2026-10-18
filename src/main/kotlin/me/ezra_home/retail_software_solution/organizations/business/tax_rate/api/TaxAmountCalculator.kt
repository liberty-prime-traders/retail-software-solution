package me.ezra_home.retail_software_solution.organizations.business.tax_rate.api

import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxCalculationMethod
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal
import java.util.UUID

object TaxAmountCalculator {

    data class ActiveTaxTypeForCalculation(
        val orgJurisdictionTaxTypeId: UUID,
        val jurisdictionTaxTypeId: UUID,
        val calculationMethod: TaxCalculationMethod,
        val activeTaxRateDto: ActiveTaxRateDto
    )

    data class TaxCalculationEntry(
        val orgJurisdictionTaxTypeId: UUID,
        val jurisdictionTaxTypeId: UUID,
        val calculationMethod: TaxCalculationMethod,
        val rate: BigDecimal,
        val taxIsBilledToCustomerSeparately: Boolean,
        val taxIsIncludedInTaxableAmount: Boolean,
        val taxableAmount: BigDecimal,
        val resolvedTaxableBase: BigDecimal,
        val taxAmount: BigDecimal
    )

    data class TaxCalculationResult(
        val entries: List<TaxCalculationEntry>,
        val taxBilled: BigDecimal
    )

    fun calculate(
        taxableAmount: BigDecimal,
        activeTaxTypes: List<ActiveTaxTypeForCalculation>
    ): TaxCalculationResult {
        val resolvedTaxableBase = resolveTaxableBase(taxableAmount, activeTaxTypes)

        val entries = activeTaxTypes.map { activeTaxType ->
            TaxCalculationEntry(
                orgJurisdictionTaxTypeId = activeTaxType.orgJurisdictionTaxTypeId,
                jurisdictionTaxTypeId = activeTaxType.jurisdictionTaxTypeId,
                calculationMethod = activeTaxType.calculationMethod,
                rate = activeTaxType.activeTaxRateDto.ratePercentage ?: activeTaxType.activeTaxRateDto.rateFlatAmount ?: BigDecimal.ZERO,
                taxIsBilledToCustomerSeparately = activeTaxType.activeTaxRateDto.taxIsBilledToCustomerSeparately,
                taxIsIncludedInTaxableAmount = activeTaxType.activeTaxRateDto.taxIsIncludedInTaxableAmount,
                taxableAmount = taxableAmount,
                resolvedTaxableBase = resolvedTaxableBase,
                taxAmount = taxAmountFor(activeTaxType, resolvedTaxableBase)
            )
        }

        return TaxCalculationResult(
            entries = entries,
            taxBilled = entries.filter { it.taxIsBilledToCustomerSeparately }.fold(BigDecimal.ZERO) { acc, entry -> acc + entry.taxAmount }
        )
    }

    private fun resolveTaxableBase(taxableAmount: BigDecimal, activeTaxTypes: List<ActiveTaxTypeForCalculation>): BigDecimal {
        val included = activeTaxTypes.filter { it.activeTaxRateDto.taxIsIncludedInTaxableAmount }
        val includedFlatAmounts = included
            .filter { it.calculationMethod == TaxCalculationMethod.FIXED_VALUE }
            .fold(BigDecimal.ZERO) { acc, taxType -> acc + requireFlatAmount(taxType) }
        val includedPercentageSum = included
            .filter { it.calculationMethod == TaxCalculationMethod.PERCENTAGE }
            .fold(BigDecimal.ZERO) { acc, taxType -> acc + requirePercentage(taxType) }
        return Decimals.Scale4.divide((taxableAmount - includedFlatAmounts) * BigDecimal(100), BigDecimal(100) + includedPercentageSum)
    }

    private fun taxAmountFor(activeTaxType: ActiveTaxTypeForCalculation, netBase: BigDecimal): BigDecimal =
        when (activeTaxType.calculationMethod) {
            TaxCalculationMethod.PERCENTAGE ->
                Decimals.Scale4.divide(netBase * requirePercentage(activeTaxType), BigDecimal(100))
            TaxCalculationMethod.FIXED_VALUE ->
                Decimals.Scale4.round(requireFlatAmount(activeTaxType))
        }

    private fun requirePercentage(activeTaxType: ActiveTaxTypeForCalculation): BigDecimal =
        activeTaxType.activeTaxRateDto.ratePercentage ?: throw RtsGenericException("Tax rate missing ratePercentage")

    private fun requireFlatAmount(activeTaxType: ActiveTaxTypeForCalculation): BigDecimal =
        activeTaxType.activeTaxRateDto.rateFlatAmount ?: throw RtsGenericException("Tax rate missing rateFlatAmount")
}
