package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.lock.api.LockNamespaces
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale.SaleAssembler
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleValidator
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidRepository
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleStockUpdater
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockReserver
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
    private val stockReserver: StockReserver,
    private val saleAssembler: SaleAssembler,
    private val saleStockUpdater: SaleStockUpdater,
    private val saleVoidHandlerForKafka: SaleVoidHandlerForKafka,
    private val saleValidator: SaleValidator,
    private val saleVoidRepository: SaleVoidRepository,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val fiscalPeriodService: FiscalPeriodService,
) {

    fun updatePaymentStatus(id: UUID, status: PaymentStatus): Long {
        val sale = saleDataFetcher.lockAndGetSale(id)
        sale.paymentStatus = status
        return saleRepository.saveAndFlush(sale).version
    }

    fun updateNotes(id: UUID, notes: String?) {
        val sale = saleDataFetcher.lockAndGetSale(id)
        sale.notes = notes
        saleRepository.save(sale)
    }

    fun voidSale(saleVoidCreateDto: SaleVoidCreateDto): SaleSummary {
        val sale = saleDataFetcher.lockAndGetSale(saleVoidCreateDto.saleId)
        saleValidator.guardCanVoid(sale)
        val saleLines = saleLineRepository.findBySaleId(saleVoidCreateDto.saleId)
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, saleLines.map { it.locationProductId })
        if (sale.status == SaleStatus.DRAFT) {
            stockReserver.clearBySale(saleVoidCreateDto.saleId)
            sale.status = SaleStatus.DISCARDED
        } else {
            fiscalPeriodService.requireOpenForDate(DateTimes.Local.Now.organization())
            saleStockUpdater.restoreStock(sale.requiredReference())
            sale.status = SaleStatus.VOIDED
            val saleVoidEntity = saleVoidRepository.save(
                SaleVoidEntity(saleId = saleVoidCreateDto.saleId, reason = saleVoidCreateDto.reason)
            )
            saleVoidHandlerForKafka.publishVoid(sale, saleVoidEntity)
        }
        saleRepository.save(sale)
        return saleAssembler.buildSummary(sale)
    }

}
