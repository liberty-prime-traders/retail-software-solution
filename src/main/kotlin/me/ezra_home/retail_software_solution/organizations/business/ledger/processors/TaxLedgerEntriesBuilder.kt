package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeFetcher
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeStatus
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.api.ActiveTaxRateDto
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.api.TaxRateService
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeFetcher
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxApplicationLevel
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxTrigger
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class TaxLedgerEntriesBuilder(
    private val taxRateService: TaxRateService,
    private val orgJurisdictionTaxTypeFetcher: OrgJurisdictionTaxTypeFetcher,
    private val jurisdictionTaxTypeFetcher: JurisdictionTaxTypeFetcher
) {

    fun buildTransactionLevelTaxEntries(
        eventDate: LocalDate,
        amountToTax: BigDecimal,
        taxTrigger: TaxTrigger
    ): List<LedgerEntryRequest> {
        val activeTaxTypes = orgJurisdictionTaxTypeFetcher.getAllDtos()
            .filter { it.status == OrgJurisdictionTaxTypeStatus.ACTIVE }
        val rates = taxRateService.findActiveRateForDate(eventDate)
        return activeTaxTypes.flatMap { orgTaxType ->
            val taxType = jurisdictionTaxTypeFetcher.getTaxType(orgTaxType.jurisdictionTaxTypeId)

            if (taxType.taxApplicationLevel != TaxApplicationLevel.TRANSACTION) return@flatMap emptyList()
            if (taxTrigger !in taxType.taxTriggers) return@flatMap emptyList()

            val rate = rates[orgTaxType.id] ?: return@flatMap emptyList()
            val taxAmount = calculate(taxType.calculationMethod, rate, amountToTax)
            listOf(
                LedgerEntryRequest(SystemAccount.GROSS_SALES.code, EntryType.DEBIT, taxAmount),
                LedgerEntryRequest(orgTaxType.payableAccountCode, EntryType.CREDIT, taxAmount)
            )
        }
    }

    private fun calculate(calculationMethod: CalculationMethod, rate: ActiveTaxRateDto, amountToTax: BigDecimal): BigDecimal {
        return when (calculationMethod) {
            CalculationMethod.PERCENTAGE -> {
                val pct = rate.ratePercentage ?: throw RtsGenericException("Tax rate is missing ratePercentage for PERCENTAGE calculation")
                Decimals.multiplyScale4(amountToTax, Decimals.divideScale4(pct, BigDecimal(100)))
            }
            CalculationMethod.FIXED_VALUE -> {
                val flat = rate.rateFlatAmount ?: throw RtsGenericException("Tax rate is missing rateFlatAmount for FIXED_VALUE calculation")
                flat.setScale(4, RoundingMode.HALF_UP)
            }
        }
    }
}
