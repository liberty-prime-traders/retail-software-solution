package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.lock.api.LockNamespaces
import me.ezra_home.retail_software_solution.locations.business.sale.SaleCommitLineSync
import me.ezra_home.retail_software_solution.locations.business.sale.SaleConfirmedHandlerForKafka
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
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleLineStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleStockUpdater
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class SaleConfirmCommitter(
    private val saleRepository: SaleRepository,
    private val saleLineRepository: SaleLineRepository,
    private val saleStockReserver: SaleStockReserver,
    private val saleStockUpdater: SaleStockUpdater,
    private val saleConfirmedHandlerForKafka: SaleConfirmedHandlerForKafka,
    private val locationProductService: LocationProductService,
    private val fiscalPeriodService: FiscalPeriodService,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val saleAdjustmentCommitter: SaleAdjustmentCommitter,
    private val saleAdjustmentFetcher: SaleAdjustmentFetcher,
    private val salePaymentCommitter: SalePaymentCommitter,
    private val saleTotalsApplier: SaleTotalsApplier,
) {

    fun confirm(input: SaleCommitInput): SaleCommitOutcome {
        if (input.lines.isEmpty()) throw RtsGenericException("Sale must have at least one line")
        salePaymentCommitter.ensureRemovalsRejected(input)
        locationProductService.guardAllActive(input.lines.map { it.locationProductId })
        val effectiveDate = input.dateSold ?: DateTimes.Offset.Now.organization()
        SaleValidator.guardDateSoldIsNotFuture(effectiveDate)
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(effectiveDate))

        val sale = loadOrCreate(input, effectiveDate)
        sale.contactId = input.contactId
        sale.soldById = input.soldById ?: SessionContextProvider.getUserId()
        sale.dateSold = effectiveDate
        sale.notes = input.notes
        saleRepository.save(sale)

        val lineSyncResult = SaleCommitLineSync.sync(sale, input, saleLineRepository)
        val productIds = lineSyncResult.persistedLines.mapTo(HashSet()) { it.locationProductId }
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, productIds)
        saleStockReserver.clearBySale(sale.id!!)

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
        if (input.contactId == SystemContact.WALK_IN.id) {
            salePaymentCommitter.guardFullCoverage(sale.id!!, payableTotal, input)
        }
        val paymentResult = salePaymentCommitter.appendNew(
            saleId = sale.id!!, contactId = sale.contactId, payableTotal = payableTotal, input = input,
        )
        sale.paymentStatus = paymentResult.newStatus
        sale.status = SaleStatus.CONFIRMED
        saleRepository.save(sale)

        runFifoConsumption(sale, lineSyncResult.persistedLines)
        saleConfirmedHandlerForKafka.publish(sale)

        return SaleCommitOutcome(
            saleId = sale.id!!,
            saleReferenceNumber = sale.requiredReference(),
            newVersion = sale.version,
            lineIdsByClientKey = lineSyncResult.lineIdByClientKey,
            adjustmentIdsByClientKey = adjustmentIdByClientKey,
            paymentIdsByClientKey = paymentResult.idsByClientKey,
        )
    }

    private fun runFifoConsumption(
        sale: SaleEntity,
        lines: List<me.ezra_home.retail_software_solution.locations.business.sale.SaleLineEntity>,
    ) {
        val requests = lines.map { line ->
            SaleLineStockRequest(
                saleLineId = line.id!!,
                locationProductId = line.locationProductId,
                baseQuantity = line.baseQty(),
                unitId = line.unitId,
                conversionFactor = line.conversionFactor,
            )
        }
        saleStockUpdater.consumeStock(requests, sale.requiredReference())
    }

    private fun loadOrCreate(input: SaleCommitInput, effectiveDate: java.time.OffsetDateTime): SaleEntity {
        val saleId = input.saleId ?: return SaleEntity(
            contactId = input.contactId,
            soldById = input.soldById,
            dateSold = effectiveDate,
            notes = input.notes,
            status = SaleStatus.CONFIRMED,
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
}
