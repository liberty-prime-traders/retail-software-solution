package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.SaleAssembler
import me.ezra_home.retail_software_solution.locations.business.sale.SaleConfirmedHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLinePreparer
import me.ezra_home.retail_software_solution.locations.business.sale.SaleMutator
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleStockReserver
import me.ezra_home.retail_software_solution.locations.business.sale.SaleValidator
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentService
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleLineStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleStockUpdater
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class SaleConfirmationHandler(
    private val saleRepository: SaleRepository,
    private val saleAssembler: SaleAssembler,
    private val saleLinePreparer: SaleLinePreparer,
    private val saleMutator: SaleMutator,
    private val saleValidator: SaleValidator,
    private val saleStockReserver: SaleStockReserver,
    private val saleStockUpdater: SaleStockUpdater,
    private val saleConfirmedHandlerForKafka: SaleConfirmedHandlerForKafka,
    private val fiscalPeriodService: FiscalPeriodService,
    private val salePaymentService: SalePaymentService,
) {

    fun createSale(dto: SaleCreateDto): SaleResponseDto {
        val validated = saleLinePreparer.prepareForCreate(dto)
        val dateSold = dto.dateSold ?: DateTimes.Offset.Now.organization()
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(dateSold))
        val sale = SaleEntity(
            contactId = dto.resolveContactId(),
            soldBy = dto.soldBy ?: SessionContextProvider.getUserId(),
            dateSold = dateSold,
            notes = dto.notes,
            status = SaleStatus.CONFIRMED,
        )
        val outcome = saleMutator.create(dto, sale, validated)
        runFifoConsumption(outcome.lines, sale.requiredReference())
        saleConfirmedHandlerForKafka.publish(sale, outcome.lines, outcome.discounts)
        return saleAssembler.buildResponse(sale, outcome.lines, validated.productSummaries)
    }

    fun convertDraftToSale(dto: SaleUpdateDto): SaleResponseDto {
        val sale = saleRepository.getReferenceById(dto.id)
        SaleValidator.guardIsDraft(sale)
        dto.applyContactId(sale)
        sale.soldBy = dto.soldBy ?: sale.soldBy ?: SessionContextProvider.getUserId()
        sale.dateSold = dto.dateSold ?: sale.dateSold ?: DateTimes.Offset.Now.organization()
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(sale.dateSold!!))
        sale.notes = dto.notes ?: sale.notes
        val outcome = saleMutator.update(dto, sale) { lines -> SaleValidator.guardHasLines(lines) }
        if (sale.contactId == SystemContact.WALK_IN.id) {
            saleValidator.guardWalkInPaymentCoverage(dto.id, sale.subtotal!!)
        }
        sale.status = SaleStatus.CONFIRMED
        saleStockReserver.clearBySale(dto.id)

        val survivingLines = outcome.updateResult.survivingSaleLines
        runFifoConsumption(survivingLines, sale.requiredReference())
        saleConfirmedHandlerForKafka.publish(sale, survivingLines, outcome.discounts)
        salePaymentService.publishKafkaForExistingPayments(dto.id)
        return saleAssembler.buildResponse(sale, survivingLines, outcome.updateResult.productSummaries)
    }

    private fun runFifoConsumption(saleLineEntities: List<SaleLineEntity>, saleRefNumber: String) {
        val requests = saleLineEntities.map {
            SaleLineStockRequest(
                saleLineId = it.id!!,
                locationProductId = it.locationProductId,
                baseQuantity = it.baseQty(),
                unitId = it.unitId,
                conversionFactor = it.conversionFactor,
            )
        }
        saleStockUpdater.consumeStock(requests, saleRefNumber)
    }
}
