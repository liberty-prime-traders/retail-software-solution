package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.SaleSaveFinalizer
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineSync
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
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val fiscalPeriodService: FiscalPeriodService,
    private val saleSaveFinalizer: SaleSaveFinalizer,
) {

    fun saveDraft(saleSaveRequest: SaleSaveRequest): SaleSaveResult {
        guardFiscalIfPayments(saleSaveRequest)

        val saleEntity = loadOrCreate(saleSaveRequest)
        saleEntity.contactId = saleSaveRequest.contactId
        saleEntity.soldById = saleSaveRequest.soldById
        saleEntity.dateSold = saleSaveRequest.dateSold
        saleEntity.notes = saleSaveRequest.notes
        saleEntity.status = SaleStatus.DRAFT
        saleRepository.save(saleEntity)

        saleStockReserver.clearBySale(saleEntity.id!!)
        val lineSyncResult = SaleLineSync.sync(saleEntity, saleSaveRequest, saleLineRepository)
        if (lineSyncResult.persistedSaleLines.isNotEmpty()) {
            val resolvedBaseQuantitiesByLocationProductId = lineSyncResult.persistedSaleLines
                .associate { it.locationProductId to it.baseQty() }
            val productSummariesByLocationProductId = locationProductDataFetcher
                .findSummaryByIds(resolvedBaseQuantitiesByLocationProductId.keys)
            saleValidator.guardStockForDraftUpdates(
                saleEntity.id!!,
                resolvedBaseQuantitiesByLocationProductId,
                productSummariesByLocationProductId,
            )
            saleStockReserver.reserve(saleEntity.id!!, lineSyncResult.persistedSaleLines)
        }

        val saleUpdateResult = saleSaveFinalizer.finalize(
            saleEntity = saleEntity,
            saleSaveRequest = saleSaveRequest,
            lineSyncResult = lineSyncResult,
        )

        return SaleSaveResult(
            saleId = saleEntity.id!!,
            saleReferenceNumber = saleEntity.requiredReference(),
            newVersion = saleEntity.version,
            dateSold = saleEntity.dateSold,
            soldById = saleEntity.soldById,
            saleLineIdsByClientKey = lineSyncResult.saleLineIdsByClientKey,
            saleAdjustmentIdsByClientKey = saleUpdateResult.saleAdjustmentIdsByClientKey,
            persistedSalePaymentsByClientKey = saleUpdateResult.persistedSalePaymentsByClientKey,
        )
    }

    private fun loadOrCreate(saleSaveRequest: SaleSaveRequest): SaleEntity {
        val saleId = saleSaveRequest.saleId ?: return SaleEntity(contactId = saleSaveRequest.contactId, status = SaleStatus.DRAFT)
        return saleDataFetcher.loadDraftAtVersion(saleId, saleSaveRequest.expectedVersion)
    }

    private fun guardFiscalIfPayments(saleSaveRequest: SaleSaveRequest) {
        if (saleSaveRequest.salePayments.isEmpty()) return
        val effectiveDateSold = saleSaveRequest.dateSold ?: DateTimes.Offset.Now.organization()
        SaleValidator.guardDateSoldIsNotFuture(effectiveDateSold)
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(effectiveDateSold))
    }
}
