package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.SaleConfirmedHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineSync
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleSaveFinalizer
import me.ezra_home.retail_software_solution.locations.business.sale.SaleTaxFinalizer
import me.ezra_home.retail_software_solution.locations.business.sale.SaleValidator
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleLineStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleStockUpdater
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockReserver
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

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
    private val saleSaveFinalizer: SaleSaveFinalizer,
    private val saleValidator: SaleValidator,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val saleTaxFinalizer: SaleTaxFinalizer,
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
        guardStockAvailability(saleEntity.id!!, lineSyncResult.persistedSaleLines)

        saleEntity.status = SaleStatus.CONFIRMED
        val saleUpdateResult = saleSaveFinalizer.finalize(
            saleEntity = saleEntity,
            saleSaveRequest = saleSaveRequest,
            lineSyncResult = lineSyncResult,
        )

        consumeStockAndRecordCogs(saleEntity, lineSyncResult.persistedSaleLines)
        saleTaxFinalizer.finalizeTaxForConfirm(saleEntity)
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

    private fun guardStockAvailability(saleId: UUID, saleLineEntities: List<SaleLineEntity>) {
        if (saleLineEntities.isEmpty()) return
        val storageQuantitiesByLocationProductId = saleLineEntities.associate { it.locationProductId to it.storageQuantity() }
        val productSummariesByLocationProductId = locationProductDataFetcher
            .findSummaryByIds(storageQuantitiesByLocationProductId.keys)
        saleValidator.guardSufficientStockForSale(saleId, storageQuantitiesByLocationProductId, productSummariesByLocationProductId)
    }

    private fun consumeStockAndRecordCogs(
        saleEntity: SaleEntity,
        saleLineEntities: List<SaleLineEntity>,
    ) {
        val saleLineStockRequests = saleLineEntities.map { saleLineEntity ->
            SaleLineStockRequest(
                saleLineId = saleLineEntity.id!!,
                saleLineReferenceNumber = saleLineEntity.requiredReference(),
                locationProductId = saleLineEntity.locationProductId,
                userProvidedQuantity = saleLineEntity.userProvidedQuantity,
                unitId = saleLineEntity.unitId,
                storageUnitsPerUnit = saleLineEntity.storageUnitsPerUnit,
            )
        }

        val consumedCostsBySaleLineId = saleStockUpdater.consumeStock(
            saleLineStockRequests,
            saleEntity.requiredReference()
        ).associateBy { it.saleLineId }
        saleLineEntities.forEach { saleLineEntity ->
            saleLineEntity.cogsTotal = consumedCostsBySaleLineId.getValue(saleLineEntity.id!!).cost
        }
        saleLineRepository.saveAll(saleLineEntities)
    }

    private fun loadOrCreate(saleSaveRequest: SaleSaveRequest, effectiveDateSold: OffsetDateTime): SaleEntity {
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
