package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale.SaleAssembler
import me.ezra_home.retail_software_solution.locations.business.sale.SaleHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleStockReserver
import me.ezra_home.retail_software_solution.locations.business.sale.SaleValidator
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleLineStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleStockUpdater
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
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
    private val saleHandlerForKafka: SaleHandlerForKafka,
    private val saleValidator: SaleValidator,
) {

    fun updateNotes(id: UUID, notes: String?) {
        val sale = saleRepository.findById(id).orElseThrow { UpdatingNonExistingRecordException() }
        sale.notes = notes
        saleRepository.save(sale)
    }

    fun updatePaymentStatus(id: UUID, status: PaymentStatus) {
        val sale = saleRepository.getReferenceById(id)
        sale.paymentStatus = status
        saleRepository.save(sale)
    }

    fun voidSale(id: UUID): SaleResponseDto {
        val sale = saleRepository.getReferenceById(id)
        saleValidator.guardCanVoid(sale)
        val lines = saleLineRepository.findBySaleId(id)
        if (sale.status == SaleStatus.DRAFT) {
            saleStockReserver.clearBySale(id)
            sale.status = SaleStatus.DISCARDED
            saleRepository.save(sale)
        } else {
            val requests = lines.map {
                SaleLineStockRequest(it.id!!, it.locationProductId, it.quantity, it.unitId, it.conversionFactor)
            }
            saleStockUpdater.restoreStock(requests, sale.referenceNumber!!)
            sale.status = SaleStatus.VOIDED
            saleRepository.save(sale)
            saleHandlerForKafka.publishVoid(sale, lines)
        }
        return saleAssembler.buildResponse(sale, lines)
    }

}
