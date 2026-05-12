package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.lock.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale.SaleAssembler
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleStockReserver
import me.ezra_home.retail_software_solution.locations.business.sale.SaleValidator
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidRepository
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleLineStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleStockUpdater
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleUpdater(
    private val saleRepository: SaleRepository,
    private val saleLineRepository: SaleLineRepository,
    private val saleStockReserver: SaleStockReserver,
    private val saleAssembler: SaleAssembler,
    private val saleStockUpdater: SaleStockUpdater,
    private val saleVoidHandlerForKafka: SaleVoidHandlerForKafka,
    private val saleValidator: SaleValidator,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val saleVoidRepository: SaleVoidRepository,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val fiscalPeriodService: FiscalPeriodService,
) {

    fun updatePaymentStatus(id: UUID, status: PaymentStatus) {
        val sale = saleRepository.getReferenceById(id)
        sale.paymentStatus = status
    }

    fun updateNotes(id: UUID, notes: String?) {
        val sale = saleRepository.getReferenceById(id)
        sale.notes = notes
    }

    fun voidSale(dto: SaleVoidCreateDto): SaleResponseDto {
        entityAdvisoryLock.acquire(LockNamespaces.SALE, dto.saleId)
        val sale = saleRepository.getReferenceById(dto.saleId)
        saleValidator.guardCanVoid(sale)
        val lines = saleLineRepository.findBySaleId(dto.saleId)
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, lines.map { it.locationProductId })
        if (sale.status == SaleStatus.DRAFT) {
            saleStockReserver.clearBySale(dto.saleId)
            sale.status = SaleStatus.DISCARDED
        } else {
            fiscalPeriodService.requireOpenForDate(DateTimes.Local.Now.organization())
            val requests = lines.map {
                SaleLineStockRequest(it.id!!, it.locationProductId, it.quantity, it.unitId, it.conversionFactor)
            }
            saleStockUpdater.restoreStock(requests, sale.requiredReference())
            sale.status = SaleStatus.VOIDED
            val voidEntity = saleVoidRepository.save(SaleVoidEntity(saleId = dto.saleId, reason = dto.reason))
            saleVoidHandlerForKafka.publishVoid(sale, lines, voidEntity)
        }
        val productSummaries = locationProductDataFetcher.findSummaryByIds(lines.map { it.locationProductId })
        return saleAssembler.buildResponse(sale, lines, productSummaries)
    }

}
