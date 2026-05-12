package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.lock.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.lock.LockNamespaces
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
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal

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
    private val contactService: ContactService,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    fun createSale(dto: SaleCreateDto): SaleResponseDto {
        val contactId = dto.resolveContactId()
        if (!dto.walkInCustomer) contactService.guardExists(contactId)
        val dateSold = dto.dateSold ?: DateTimes.Offset.Now.organization()
        SaleValidator.guardDateSoldIsNotFuture(dateSold)
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(dateSold))
        val validatedSaleLines = saleLinePreparer.prepareForCreate(dto)
        val sale = SaleEntity(
            contactId = contactId,
            soldById = dto.soldById ?: SessionContextProvider.getUserId(),
            dateSold = dateSold,
            notes = dto.notes,
            status = SaleStatus.CONFIRMED,
        )
        val outcome = saleMutator.create(dto, sale, validatedSaleLines)
        if (dto.walkInCustomer) {
            saleValidator.guardWalkInPaymentCoverage(sale.id!!, sale.saleTotalAfterDiscounts())
        }
        runFifoConsumption(outcome.lines, sale.requiredReference())
        saleConfirmedHandlerForKafka.publish(sale, outcome.lines, outcome.discounts)
        salePaymentService.publishKafkaForExistingPayments(sale.id!!)
        return saleAssembler.buildResponse(sale, outcome.lines, validatedSaleLines.productSummaries)
    }

    fun convertDraftToSale(dto: SaleUpdateDto): SaleResponseDto {
        entityAdvisoryLock.acquire(LockNamespaces.SALE, dto.id)
        val sale = saleRepository.findById(dto.id)
            .orElseThrow { RtsGenericException("Sale ${dto.id} not found") }
        SaleValidator.guardIsDraft(sale)
        dto.applyTo(sale)
        if (sale.soldById == null) {
            sale.soldById = SessionContextProvider.getUserId()
        }
        if (sale.dateSold == null) {
            sale.dateSold = DateTimes.Offset.Now.organization()
        }
        SaleValidator.guardDateSoldIsNotFuture(sale.dateSold!!)
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(sale.dateSold!!))
        if (sale.contactId != SystemContact.WALK_IN.id) {
            contactService.guardExists(sale.contactId)
        }

        val outcome = saleMutator.update(dto, sale)
        SaleValidator.guardHasLines(outcome.survivingLines)
        if (sale.contactId == SystemContact.WALK_IN.id) {
            saleValidator.guardWalkInPaymentCoverage(dto.id, sale.saleTotalAfterDiscounts())
        }
        sale.status = SaleStatus.CONFIRMED
        saleStockReserver.clearBySale(dto.id)

        runFifoConsumption(outcome.survivingLines, sale.requiredReference())
        saleConfirmedHandlerForKafka.publish(sale, outcome.survivingLines, outcome.discounts)
        salePaymentService.publishKafkaForExistingPayments(dto.id)
        return saleAssembler.buildResponse(sale, outcome.survivingLines, outcome.productSummaries)
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
