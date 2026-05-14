package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntryCreateDto
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntryService
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxSourceType
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleConfirmedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.InventoryEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
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
import kotlin.reflect.KClass

@Service
class SaleTaxFinalizationProcessor(
    private val saleRepository: SaleRepository,
    private val taxEntryService: TaxEntryService,
    private val orgJurisdictionTaxTypeFetcher: OrgJurisdictionTaxTypeFetcher,
    private val jurisdictionTaxTypeFetcher: JurisdictionTaxTypeFetcher,
    private val taxRateService: TaxRateService,
    private val fiscalPeriodService: FiscalPeriodService
) : InventoryEventProcessor<SaleConfirmedEvent> {

    override val eventType: KClass<SaleConfirmedEvent> = SaleConfirmedEvent::class

    @TransactionalOnLocationSchema(readOnly = true)
    override fun shouldProcess(event: SaleConfirmedEvent): Boolean {
        return !taxEntryService.existsBySourceReference(event.saleReferenceNumber, TaxSourceType.SALE)
    }

    @TransactionalOnLocationSchema
    override fun handle(event: SaleConfirmedEvent) {
        val taxableAmount = event.subtotal - event.discountTotal
        val activeTaxTypes = orgJurisdictionTaxTypeFetcher.getAllDtos()
            .filter { it.status == OrgJurisdictionTaxTypeStatus.ACTIVE }
        val fiscalPeriodId = fiscalPeriodService.requireOpenForDate(event.dateSold)
        val rates = taxRateService.findActiveRateForDate(event.dateSold)

        val taxEntries = mutableListOf<TaxEntryCreateDto>()
        var totalTaxAmount = BigDecimal.ZERO
        var grandTotal = taxableAmount

        activeTaxTypes.forEach { orgTaxType ->
            val taxType = jurisdictionTaxTypeFetcher.getTaxType(orgTaxType.jurisdictionTaxTypeId)
            if (taxType.taxApplicationLevel != TaxApplicationLevel.TRANSACTION) return@forEach
            if (TaxTrigger.SALE !in taxType.taxTriggers) return@forEach
            val rate = rates[orgTaxType.id] ?: return@forEach

            val taxAmount = computeTaxAmount(taxType.calculationMethod, rate, taxableAmount, orgTaxType.taxInclusive)
            val effectiveRate = rate.ratePercentage ?: rate.rateFlatAmount ?: BigDecimal.ZERO

            taxEntries += TaxEntryCreateDto(
                sourceReferenceNumber = event.saleReferenceNumber,
                sourceType = TaxSourceType.SALE,
                taxTypeId = orgTaxType.jurisdictionTaxTypeId,
                fiscalPeriodId = fiscalPeriodId,
                calculationMethod = taxType.calculationMethod,
                rate = effectiveRate,
                taxInclusive = orgTaxType.taxInclusive,
                taxableAmount = taxableAmount,
                taxAmount = taxAmount
            )

            totalTaxAmount += taxAmount
            if (!orgTaxType.taxInclusive) grandTotal += taxAmount
        }

        taxEntryService.createAll(taxEntries)

        val sale = saleRepository.getReferenceById(event.sourceDocumentId)
        sale.taxTotal = totalTaxAmount
        sale.grandTotal = grandTotal
        saleRepository.save(sale)
    }

    private fun computeTaxAmount(
        calculationMethod: CalculationMethod,
        rate: ActiveTaxRateDto,
        taxableAmount: BigDecimal,
        taxInclusive: Boolean
    ): BigDecimal {
        return when (calculationMethod) {
            CalculationMethod.PERCENTAGE -> {
                val pct = rate.ratePercentage ?: throw RtsGenericException("Tax rate missing ratePercentage")
                val rateDecimal = Decimals.divideScale4(pct, BigDecimal(100))
                if (taxInclusive) {
                    taxableAmount - Decimals.divideScale4(taxableAmount, BigDecimal.ONE + rateDecimal)
                } else {
                    Decimals.multiplyScale4(taxableAmount, rateDecimal)
                }
            }
            CalculationMethod.FIXED_VALUE -> {
                val flat = rate.rateFlatAmount ?: throw RtsGenericException("Tax rate missing rateFlatAmount")
                flat.setScale(4, RoundingMode.HALF_UP)
            }
        }
    }
}
