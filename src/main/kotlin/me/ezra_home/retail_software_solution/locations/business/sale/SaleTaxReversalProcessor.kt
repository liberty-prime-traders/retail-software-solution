package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntryCreateDto
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntryService
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxSourceType
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleVoidedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.InventoryEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.model.ConstraintNames
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class SaleTaxReversalProcessor(
    private val taxEntryService: TaxEntryService,
    private val fiscalPeriodService: FiscalPeriodService
) : InventoryEventProcessor<SaleVoidedEvent> {

    override val eventType: KClass<SaleVoidedEvent> = SaleVoidedEvent::class
    override val idempotencyConstraintName = ConstraintNames.TAX_ENTRY_UNIQUE_SOURCE_REFERENCE_NUMBER_SOURCE_TYPE_AND_TAX_TYPE

    @TransactionalOnLocationSchema(readOnly = true)
    override fun shouldProcess(event: SaleVoidedEvent): Boolean {
        val originalsExist = taxEntryService.existsBySourceReference(
            event.saleReferenceNumber, TaxSourceType.SALE
        )
        val reversalsExist = taxEntryService.existsBySourceReference(
            event.saleReferenceNumber, TaxSourceType.SALE_VOID
        )
        return originalsExist && !reversalsExist
    }

    @TransactionalOnLocationSchema
    override fun handle(event: SaleVoidedEvent) {
        val originals = taxEntryService.findBySourceReference(
            event.saleReferenceNumber, TaxSourceType.SALE
        )

        if (originals.isEmpty()) return

        val fiscalPeriodId = fiscalPeriodService.requireOpenForDate(event.dateVoided)

        val reversals = originals.map { source ->
            TaxEntryCreateDto(
                sourceReferenceNumber = source.sourceReferenceNumber,
                sourceType = TaxSourceType.SALE_VOID,
                direction = source.direction,
                taxTypeId = source.taxTypeId,
                fiscalPeriodId = fiscalPeriodId,
                calculationMethod = source.calculationMethod,
                rate = source.rate,
                taxInclusive = source.taxInclusive,
                taxableAmount = source.taxableAmount.negate(),
                taxAmount = source.taxAmount.negate()
            )
        }
        taxEntryService.createAll(reversals)
    }
}
