package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.lock.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.locations.business.sale.SaleAssembler
import me.ezra_home.retail_software_solution.locations.business.sale.SaleConfirmedHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLinesInsertPreparer
import me.ezra_home.retail_software_solution.locations.business.sale.SaleMutator
import me.ezra_home.retail_software_solution.locations.business.sale.SaleStockReserver
import me.ezra_home.retail_software_solution.locations.business.sale.SaleValidator
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleLineStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleStockUpdater
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class SaleConfirmationHandler(
    private val saleDataFetcher: SaleDataFetcher,
    private val saleAssembler: SaleAssembler,
    private val saleLinesInsertPreparer: SaleLinesInsertPreparer,
    private val saleMutator: SaleMutator,
    private val saleValidator: SaleValidator,
    private val saleStockReserver: SaleStockReserver,
    private val saleStockUpdater: SaleStockUpdater,
    private val saleConfirmedHandlerForKafka: SaleConfirmedHandlerForKafka,
    private val fiscalPeriodService: FiscalPeriodService,
    private val contactService: ContactService,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    fun createSale(dto: SaleCreateDto): SaleResponseDto {
        val contactId = dto.resolveContactId()
        if (!dto.walkInCustomer()) contactService.guardExists(contactId)
        val dateSold = dto.dateSold ?: DateTimes.Offset.Now.organization()
        SaleValidator.guardDateSoldIsNotFuture(dateSold)
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(dateSold))
        val insertContext = saleLinesInsertPreparer.prepareForSaleConfirmation(dto)
        val sale = SaleEntity(
            contactId = contactId,
            soldById = dto.soldById ?: SessionContextProvider.getUserId(),
            dateSold = dateSold,
            notes = dto.notes,
            status = SaleStatus.CONFIRMED,
        )
        val outcome = saleMutator.create(dto, sale, insertContext)
        runFifoConsumption(outcome.lines, sale.requiredReference())
        saleConfirmedHandlerForKafka.publish(sale, outcome.lines, outcome.discounts)
        return saleAssembler.buildResponse(sale, outcome.lines, insertContext.productSummaries)
    }

    fun convertDraftToSale(dto: SaleUpdateDto): SaleResponseDto {
        SaleValidator.guardNotReassigningToWalkIn(dto)
        val sale = saleDataFetcher.lockAndGetSale(dto.id)
        SaleValidator.guardIsDraft(sale)
        saleValidator.guardSurvivingLinesNotEmpty(dto)
        dto.applyTo(sale)
        if (sale.soldById == null) {
            sale.soldById = SessionContextProvider.getUserId()
        }
        val dateSold = sale.dateSold ?: DateTimes.Offset.Now.organization()
        sale.dateSold = dateSold
        SaleValidator.guardDateSoldIsNotFuture(dateSold)
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(dateSold))
        contactService.guardExists(sale.contactId)

        val outcome = saleMutator.updateWithoutSyncingReservations(dto, sale)
        sale.status = SaleStatus.CONFIRMED

        val productIds = outcome.survivingLines.mapTo(HashSet()) { it.locationProductId }
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, productIds)
        saleStockReserver.clearBySale(dto.id)

        runFifoConsumption(outcome.survivingLines, sale.requiredReference())
        saleConfirmedHandlerForKafka.publish(sale, outcome.survivingLines, outcome.discounts)
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
