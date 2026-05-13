package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.tax_entry.TaxEntryEntity
import me.ezra_home.retail_software_solution.locations.business.tax_entry.TaxEntryRepository
import me.ezra_home.retail_software_solution.locations.business.tax_entry.TaxSourceType
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleVoidedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.InventoryEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class SaleTaxReversalProcessor(
    private val taxEntryRepository: TaxEntryRepository,
    private val fiscalPeriodService: FiscalPeriodService
) : InventoryEventProcessor<SaleVoidedEvent> {

    override val eventType: KClass<SaleVoidedEvent> = SaleVoidedEvent::class

    @TransactionalOnLocationSchema(readOnly = true)
    override fun shouldProcess(event: SaleVoidedEvent): Boolean {
        val originalsExist = taxEntryRepository.existsBySourceReferenceNumberAndSourceType(
            event.saleReferenceNumber, TaxSourceType.SALE
        )
        val reversalsExist = taxEntryRepository.existsBySourceReferenceNumberAndSourceType(
            event.saleReferenceNumber, TaxSourceType.SALE_VOID
        )
        return originalsExist && !reversalsExist
    }

    @TransactionalOnLocationSchema
    override fun handle(event: SaleVoidedEvent) {
        val originals = taxEntryRepository.findBySourceReferenceNumberAndSourceType(
            event.saleReferenceNumber, TaxSourceType.SALE
        )

        if (originals.isEmpty()) return

        val fiscalPeriodId = fiscalPeriodService.requireOpenForDate(event.dateVoided)

        val reversals = originals.map { source ->
            TaxEntryEntity(
                sourceReferenceNumber = source.sourceReferenceNumber,
                sourceType = TaxSourceType.SALE_VOID,
                taxTypeId = source.taxTypeId,
                fiscalPeriodId = fiscalPeriodId,
                calculationMethod = source.calculationMethod,
                rate = source.rate,
                taxInclusive = source.taxInclusive,
                taxableAmount = source.taxableAmount.negate(),
                taxAmount = source.taxAmount.negate()
            )
        }
        taxEntryRepository.saveAll(reversals)
    }
}
