package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.lock.api.LockNamespaces
import me.ezra_home.retail_software_solution.locations.business.sale.SaleConfirmedHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineSync
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleSaveFinalizer
import me.ezra_home.retail_software_solution.locations.business.sale.SaleValidator
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleLineStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleStockUpdater
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockReserver
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class ConfirmedSalePersister(
    private val saleRepository: SaleRepository,
    private val saleLineRepository: SaleLineRepository,
    private val stockReserver: StockReserver,
    private val saleStockUpdater: SaleStockUpdater,
    private val saleConfirmedHandlerForKafka: SaleConfirmedHandlerForKafka,
    private val saleDataFetcher: SaleDataFetcher,
    private val fiscalPeriodService: FiscalPeriodService,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val saleSaveFinalizer: SaleSaveFinalizer,
) {

    fun confirm(saleSaveRequest: SaleSaveRequest): SaleSaveResult {
        val effectiveDateSold = saleSaveRequest.dateSold ?: DateTimes.Offset.Now.organization()
        SaleValidator.guardDateSoldIsNotFuture(effectiveDateSold)
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(effectiveDateSold))

        val saleEntity = loadOrCreate(saleSaveRequest, effectiveDateSold)
        saleEntity.contactId = saleSaveRequest.contactId
        saleEntity.soldById = saleSaveRequest.soldById ?: SessionContextProvider.getUserId()
        saleEntity.dateSold = effectiveDateSold
        saleEntity.notes = saleSaveRequest.notes
        saleRepository.save(saleEntity)

        stockReserver.clearBySale(saleEntity.id!!)
        val lineSyncResult = SaleLineSync.sync(saleEntity, saleSaveRequest, saleLineRepository)
        val locationProductIdsToLock = lineSyncResult.persistedSaleLines.mapTo(HashSet()) { it.locationProductId }
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductIdsToLock)

        saleEntity.status = SaleStatus.CONFIRMED
        val saleUpdateResult = saleSaveFinalizer.finalize(
            saleEntity = saleEntity,
            saleSaveRequest = saleSaveRequest,
            lineSyncResult = lineSyncResult,
        )

        runFifoConsumption(saleEntity, lineSyncResult.persistedSaleLines)
        saleConfirmedHandlerForKafka.publish(saleEntity)

        return SaleSaveResult(
            saleId = saleEntity.id!!,
            saleReferenceNumber = saleEntity.requiredReference(),
            newVersion = saleEntity.version,
            saleStatus = saleEntity.status,
            dateSold = saleEntity.dateSold,
            soldById = saleEntity.soldById,
            saleLineIdsByClientKey = lineSyncResult.saleLineIdsByClientKey,
            saleAdjustmentIdsByClientKey = saleUpdateResult.saleAdjustmentIdsByClientKey,
            persistedSalePaymentsByClientKey = saleUpdateResult.persistedSalePaymentsByClientKey,
        )
    }

    private fun runFifoConsumption(
        saleEntity: SaleEntity,
        saleLineEntities: List<SaleLineEntity>,
    ) {
        val saleLineStockRequests = saleLineEntities.map { saleLineEntity ->
            SaleLineStockRequest(
                saleLineId = saleLineEntity.id!!,
                locationProductId = saleLineEntity.locationProductId,
                baseQuantity = saleLineEntity.baseQty(),
                unitId = saleLineEntity.unitId,
                conversionFactor = saleLineEntity.conversionFactor,
            )
        }

        saleStockUpdater.consumeStock(
            saleLineStockRequests,
            saleEntity.requiredReference()
        )
    }

    private fun loadOrCreate(saleSaveRequest: SaleSaveRequest, effectiveDateSold: java.time.OffsetDateTime): SaleEntity {
        val saleId = saleSaveRequest.saleId ?: return SaleEntity(
            contactId = saleSaveRequest.contactId,
            soldById = saleSaveRequest.soldById,
            dateSold = effectiveDateSold,
            notes = saleSaveRequest.notes,
            status = SaleStatus.CONFIRMED,
        )
        return saleDataFetcher.loadDraftAtVersion(saleId, saleSaveRequest.expectedVersion)
    }
}
