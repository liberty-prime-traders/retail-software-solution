package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.lock.api.LockNamespaces
import me.ezra_home.retail_software_solution.locations.business.sale.SaleCommitFinalizer
import me.ezra_home.retail_software_solution.locations.business.sale.SaleCommitLineSync
import me.ezra_home.retail_software_solution.locations.business.sale.SaleConfirmedHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleStockReserver
import me.ezra_home.retail_software_solution.locations.business.sale.SaleValidator
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleLineStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleStockUpdater
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class ConfirmedSalePersister(
    private val saleRepository: SaleRepository,
    private val saleLineRepository: SaleLineRepository,
    private val saleStockReserver: SaleStockReserver,
    private val saleStockUpdater: SaleStockUpdater,
    private val saleConfirmedHandlerForKafka: SaleConfirmedHandlerForKafka,
    private val saleDataFetcher: SaleDataFetcher,
    private val fiscalPeriodService: FiscalPeriodService,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val saleCommitFinalizer: SaleCommitFinalizer,
) {

    fun confirm(input: SaleCommitInput): SaleCommitOutcome {
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

        sale.status = SaleStatus.CONFIRMED
        val finalization = saleCommitFinalizer.finalize(
            sale = sale,
            input = input,
            lineSyncResult = lineSyncResult,
        )

        runFifoConsumption(sale, lineSyncResult.persistedLines)
        saleConfirmedHandlerForKafka.publish(sale)

        return SaleCommitOutcome(
            saleId = sale.id!!,
            saleReferenceNumber = sale.requiredReference(),
            newVersion = sale.version,
            lineIdsByClientKey = lineSyncResult.saleLineIdByClientKey,
            adjustmentIdsByClientKey = finalization.adjustmentIdsByClientKey,
            paymentIdsByClientKey = finalization.paymentIdsByClientKey,
        )
    }

    private fun runFifoConsumption(
        sale: SaleEntity,
        lines: List<SaleLineEntity>,
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
        return saleDataFetcher.loadDraftAtVersion(saleId, input.expectedVersion)
    }
}
