package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.sale.SaleCommitLineSync
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleStockReserver
import me.ezra_home.retail_software_solution.locations.business.sale.SaleTotalsApplier
import me.ezra_home.retail_software_solution.locations.business.sale.SaleValidator
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.PersistedCommitLine
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentCommitter
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentCommitter
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class SaleDraftCommitter(
    private val saleRepository: SaleRepository,
    private val saleLineRepository: SaleLineRepository,
    private val saleStockReserver: SaleStockReserver,
    private val saleValidator: SaleValidator,
    private val locationProductService: LocationProductService,
    private val fiscalPeriodService: FiscalPeriodService,
    private val saleAdjustmentCommitter: SaleAdjustmentCommitter,
    private val saleAdjustmentFetcher: SaleAdjustmentFetcher,
    private val salePaymentCommitter: SalePaymentCommitter,
    private val saleTotalsApplier: SaleTotalsApplier,
) {

    fun saveDraft(input: SaleCommitInput): SaleCommitOutcome {
        salePaymentCommitter.ensureRemovalsRejected(input)
        locationProductService.guardAllActive(input.lines.map { it.locationProductId })
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

        val persistedAsCommitLines = lineSyncResult.persistedLines.map { line ->
            PersistedCommitLine(
                id = line.id!!,
                locationProductId = line.locationProductId,
                quantity = line.quantity,
                unitPrice = line.unitPrice,
            )
        }
        val adjustmentIdByClientKey = saleAdjustmentCommitter.sync(
            saleId = sale.id!!,
            input = input,
            persistedLines = persistedAsCommitLines,
            lineIdByClientKey = lineSyncResult.lineIdByClientKey,
        )
        saleTotalsApplier.applyTotals(
            sale, lineSyncResult.persistedLines,
            saleAdjustmentFetcher.getAdjustmentSummaries(sale.id!!),
        )

        val payableTotal = sale.payableTotal()
        salePaymentCommitter.guardWithinTotal(sale.id!!, payableTotal, input)
        val paymentResult = salePaymentCommitter.appendNew(
            saleId = sale.id!!, contactId = sale.contactId, payableTotal = payableTotal, input = input,
        )
        sale.paymentStatus = paymentResult.newStatus
        saleRepository.save(sale)

        return SaleCommitOutcome(
            saleId = sale.id!!,
            saleReferenceNumber = sale.requiredReference(),
            newVersion = sale.version,
            lineIdsByClientKey = lineSyncResult.lineIdByClientKey,
            adjustmentIdsByClientKey = adjustmentIdByClientKey,
            paymentIdsByClientKey = paymentResult.idsByClientKey,
        )
    }

    private fun loadOrCreate(input: SaleCommitInput): SaleEntity {
        val saleId = input.saleId ?: return SaleEntity(
            contactId = input.contactId,
            soldById = input.soldById,
            dateSold = input.dateSold,
            notes = input.notes,
            status = SaleStatus.DRAFT,
        )
        val sale = saleRepository.findById(saleId).orElseThrow {
            RtsGenericException("Sale $saleId no longer exists")
        }
        if (sale.status != SaleStatus.DRAFT) {
            throw RtsGenericException("Sale ${sale.requiredReference()} is not a draft")
        }
        val expected = input.expectedVersion
            ?: throw RtsGenericException("Expected version must be supplied when committing an existing sale")
        if (sale.version != expected) {
            throw ObjectOptimisticLockingFailureException(SaleEntity::class.java, saleId)
        }
        return sale
    }

    private fun guardFiscalIfPayments(input: SaleCommitInput) {
        if (input.payments.isEmpty()) return
        val date = input.dateSold ?: DateTimes.Offset.Now.organization()
        SaleValidator.guardDateSoldIsNotFuture(date)
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(date))
    }
}
