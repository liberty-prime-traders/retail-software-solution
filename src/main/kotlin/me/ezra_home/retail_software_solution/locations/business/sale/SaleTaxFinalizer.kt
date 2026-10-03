package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxDirection
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntryCreateDto
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntryService
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxSourceType
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeFetcher
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeStatus
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.api.TaxAmountCalculator
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.api.TaxRateService
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeFetcher
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxApplicationLevel
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxTrigger
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

// We will need to handle item-level taxes in the future, but for now, we will only handle transaction-level taxes.
@Service
@TransactionalOnLocationSchema
class SaleTaxFinalizer(
    private val saleRepository: SaleRepository,
    private val taxEntryService: TaxEntryService,
    private val orgJurisdictionTaxTypeFetcher: OrgJurisdictionTaxTypeFetcher,
    private val jurisdictionTaxTypeFetcher: JurisdictionTaxTypeFetcher,
    private val taxRateService: TaxRateService,
    private val fiscalPeriodService: FiscalPeriodService
) {

    fun finalizeTaxForConfirm(saleEntity: SaleEntity) {
        val calculationDate = DateTimes.Local.atOrganizationZone(requireNotNull(saleEntity.dateSold))
        val fiscalPeriodId = fiscalPeriodService.requireOpenForDate(calculationDate)

        val activeTaxTypes = transactionLevelActiveTaxTypesForCalculation(calculationDate)
        val result = TaxAmountCalculator.calculate(saleEntity.taxableAmount(), activeTaxTypes)

        val namesByJurisdictionTaxTypeId = jurisdictionTaxTypeFetcher.getNames(result.entries.map { it.jurisdictionTaxTypeId })
        val taxEntries = result.entries.map { entry ->
            val names = namesByJurisdictionTaxTypeId.getValue(entry.jurisdictionTaxTypeId)
            TaxEntryCreateDto(
                sourceReferenceNumber = saleEntity.requiredReference(),
                sourceType = TaxSourceType.SALE,
                direction = TaxDirection.OUTPUT,
                taxTypeId = entry.jurisdictionTaxTypeId,
                taxTypeName = names.taxTypeName,
                jurisdictionName = names.jurisdictionName,
                fiscalPeriodId = fiscalPeriodId,
                calculationMethod = entry.calculationMethod,
                rate = entry.rate,
                taxIsBilledToCustomerSeparately = entry.taxIsBilledToCustomerSeparately,
                taxIsIncludedInTaxableAmount = entry.taxIsIncludedInTaxableAmount,
                taxableAmount = entry.taxableAmount,
                resolvedTaxableBase = entry.resolvedTaxableBase,
                taxAmount = entry.taxAmount
            )
        }
        taxEntryService.createAll(taxEntries)

        saleEntity.taxTotal = result.entries.fold(BigDecimal.ZERO) { acc, entry -> acc + entry.taxAmount }
        saleEntity.taxBilled = result.taxBilled
        saleRepository.save(saleEntity)
    }

    fun finalizeTaxForVoid(saleEntity: SaleEntity, voidDate: LocalDate) {
        val originals = taxEntryService.findBySourceReference(saleEntity.requiredReference(), TaxSourceType.SALE)
        if (originals.isEmpty()) return
        val fiscalPeriodId = fiscalPeriodService.requireOpenForDate(voidDate)
        val reversals = originals.map { original ->
            TaxEntryCreateDto(
                sourceReferenceNumber = original.sourceReferenceNumber,
                sourceType = TaxSourceType.SALE_VOID,
                direction = original.direction,
                taxTypeId = original.taxTypeId,
                taxTypeName = original.taxTypeName,
                jurisdictionName = original.jurisdictionName,
                fiscalPeriodId = fiscalPeriodId,
                calculationMethod = original.calculationMethod,
                rate = original.rate,
                taxIsBilledToCustomerSeparately = original.taxIsBilledToCustomerSeparately,
                taxIsIncludedInTaxableAmount = original.taxIsIncludedInTaxableAmount,
                taxableAmount = original.taxableAmount.negate(),
                resolvedTaxableBase = original.resolvedTaxableBase.negate(),
                taxAmount = original.taxAmount.negate()
            )
        }
        taxEntryService.createAll(reversals)
    }

    private fun transactionLevelActiveTaxTypesForCalculation(calculationDate: LocalDate): List<TaxAmountCalculator.ActiveTaxTypeForCalculation> {
        val activeOrgTaxTypes = orgJurisdictionTaxTypeFetcher.getAllDtos()
            .filter { it.status == OrgJurisdictionTaxTypeStatus.ACTIVE }
        val rates = taxRateService.findActiveRateForDate(calculationDate)

        return activeOrgTaxTypes.mapNotNull { orgTaxType ->
            val taxType = jurisdictionTaxTypeFetcher.getTaxType(orgTaxType.jurisdictionTaxTypeId)
            if (taxType.taxApplicationLevel != TaxApplicationLevel.TRANSACTION) return@mapNotNull null
            if (TaxTrigger.SALE !in taxType.taxTriggers) return@mapNotNull null
            val rate = rates[orgTaxType.id] ?: return@mapNotNull null
            TaxAmountCalculator.ActiveTaxTypeForCalculation(
                orgJurisdictionTaxTypeId = orgTaxType.id,
                jurisdictionTaxTypeId = orgTaxType.jurisdictionTaxTypeId,
                calculationMethod = taxType.calculationMethod,
                activeTaxRateDto = rate
            )
        }
    }
}
