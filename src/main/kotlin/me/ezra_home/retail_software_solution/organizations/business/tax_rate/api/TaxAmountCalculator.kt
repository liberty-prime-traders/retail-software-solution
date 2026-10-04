package me.ezra_home.retail_software_solution.organizations.business.tax_rate.api

import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

object TaxAmountCalculator {

    data class ActiveTaxTypeForCalculation(
        val orgJurisdictionTaxTypeId: UUID,
        val jurisdictionTaxTypeId: UUID,
        val calculationMethod: CalculationMethod,
        val activeTaxRateDto: ActiveTaxRateDto
    )

    data class TaxCalculationEntry(
        val orgJurisdictionTaxTypeId: UUID,
        val jurisdictionTaxTypeId: UUID,
        val calculationMethod: CalculationMethod,
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
            .filter { it.calculationMethod == CalculationMethod.FIXED_VALUE }
            .fold(BigDecimal.ZERO) { acc, taxType -> acc + requireFlatAmount(taxType) }
        val includedPercentageSum = included
            .filter { it.calculationMethod == CalculationMethod.PERCENTAGE }
            .fold(BigDecimal.ZERO) { acc, taxType -> acc + requirePercentage(taxType) }
        val includedPercentageRateSum = Decimals.divideScale4(includedPercentageSum, BigDecimal(100))
        return Decimals.divideScale4(taxableAmount - includedFlatAmounts, BigDecimal.ONE + includedPercentageRateSum)
    }

    private fun taxAmountFor(activeTaxType: ActiveTaxTypeForCalculation, netBase: BigDecimal): BigDecimal =
        when (activeTaxType.calculationMethod) {
            CalculationMethod.PERCENTAGE ->
                Decimals.multiplyScale4(netBase, Decimals.divideScale4(requirePercentage(activeTaxType), BigDecimal(100)))
            CalculationMethod.FIXED_VALUE ->
                requireFlatAmount(activeTaxType).setScale(4, RoundingMode.HALF_UP)
        }

    private fun requirePercentage(activeTaxType: ActiveTaxTypeForCalculation): BigDecimal =
        activeTaxType.activeTaxRateDto.ratePercentage ?: throw RtsGenericException("Tax rate missing ratePercentage")

    private fun requireFlatAmount(activeTaxType: ActiveTaxTypeForCalculation): BigDecimal =
        activeTaxType.activeTaxRateDto.rateFlatAmount ?: throw RtsGenericException("Tax rate missing rateFlatAmount")
}
