package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.lock.api.LockNamespaces
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale.SaleAssembler
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleStockReserver
import me.ezra_home.retail_software_solution.locations.business.sale.SaleValidator
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidRepository
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleStockUpdater
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleUpdater(
    private val saleDataFetcher: SaleDataFetcher,
    private val saleRepository: SaleRepository,
    private val saleLineRepository: SaleLineRepository,
    private val saleStockReserver: SaleStockReserver,
    private val saleAssembler: SaleAssembler,
    private val saleStockUpdater: SaleStockUpdater,
    private val saleVoidHandlerForKafka: SaleVoidHandlerForKafka,
    private val saleValidator: SaleValidator,
    private val saleVoidRepository: SaleVoidRepository,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val fiscalPeriodService: FiscalPeriodService,
) {

    fun updatePaymentStatus(id: UUID, status: PaymentStatus) {
        val sale = saleDataFetcher.lockAndGetSale(id)
        sale.paymentStatus = status
        saleRepository.save(sale)
    }

    fun updateNotes(id: UUID, notes: String?) {
        val sale = saleDataFetcher.lockAndGetSale(id)
        sale.notes = notes
        saleRepository.save(sale)
    }

    fun voidSale(dto: SaleVoidCreateDto): SaleResponseDto {
        val sale = saleDataFetcher.lockAndGetSale(dto.saleId)
        saleValidator.guardCanVoid(sale)
        val lines = saleLineRepository.findBySaleId(dto.saleId)
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, lines.map { it.locationProductId })
        if (sale.status == SaleStatus.DRAFT) {
            saleStockReserver.clearBySale(dto.saleId)
            sale.status = SaleStatus.DISCARDED
        } else {
            fiscalPeriodService.requireOpenForDate(DateTimes.Local.Now.organization())
            saleStockUpdater.restoreStock(sale.requiredReference())
            sale.status = SaleStatus.VOIDED
            val voidEntity = saleVoidRepository.save(SaleVoidEntity(saleId = dto.saleId, reason = dto.reason))
            saleVoidHandlerForKafka.publishVoid(sale, voidEntity)
        }
        saleRepository.save(sale)
        return saleAssembler.buildResponse(sale)
    }

}
