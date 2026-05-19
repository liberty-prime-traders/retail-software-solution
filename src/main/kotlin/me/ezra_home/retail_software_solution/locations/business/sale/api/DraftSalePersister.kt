package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale.SaleCommitFinalizer
import me.ezra_home.retail_software_solution.locations.business.sale.SaleCommitLineSync
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleStockReserver
import me.ezra_home.retail_software_solution.locations.business.sale.SaleValidator
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class DraftSalePersister(
    private val saleRepository: SaleRepository,
    private val saleLineRepository: SaleLineRepository,
    private val saleStockReserver: SaleStockReserver,
    private val saleValidator: SaleValidator,
    private val saleDataFetcher: SaleDataFetcher,
    private val fiscalPeriodService: FiscalPeriodService,
    private val saleCommitFinalizer: SaleCommitFinalizer,
) {

    fun saveDraft(input: SaleCommitInput): SaleCommitOutcome {
        guardFiscalIfPayments(input)

        val sale = loadOrCreate(input)
        sale.contactId = input.contactId
        sale.soldById = input.soldById
        sale.dateSold = input.dateSold
        sale.notes = input.notes
        sale.status = SaleStatus.DRAFT
        saleRepository.save(sale)

        val lineSyncResult = SaleCommitLineSync.sync(sale, input, saleLineRepository)
        saleStockReserver.clearBySale(sale.id!!)
        if (lineSyncResult.persistedLines.isNotEmpty()) {
            val resolvedBaseQty = lineSyncResult.persistedLines
                .associate { it.locationProductId to it.baseQty() }
            saleValidator.guardStockForDraftUpdates(sale.id!!, resolvedBaseQty, emptyMap())
            saleStockReserver.reserve(sale.id!!, lineSyncResult.persistedLines)
        }

        val finalization = saleCommitFinalizer.finalize(
            sale = sale,
            input = input,
            lineSyncResult = lineSyncResult,
        )

        return SaleCommitOutcome(
            saleId = sale.id!!,
            saleReferenceNumber = sale.requiredReference(),
            newVersion = sale.version,
            lineIdsByClientKey = lineSyncResult.saleLineIdByClientKey,
            adjustmentIdsByClientKey = finalization.adjustmentIdsByClientKey,
            paymentIdsByClientKey = finalization.paymentIdsByClientKey,
        )
    }

    private fun loadOrCreate(input: SaleCommitInput): SaleEntity {
        val saleId = input.saleId ?: return SaleEntity(contactId = input.contactId, status = SaleStatus.DRAFT)
        return saleDataFetcher.loadDraftAtVersion(saleId, input.expectedVersion)
    }

    private fun guardFiscalIfPayments(input: SaleCommitInput) {
        if (input.payments.isEmpty()) return
        val date = input.dateSold ?: DateTimes.Offset.Now.organization()
        SaleValidator.guardDateSoldIsNotFuture(date)
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(date))
    }
}
