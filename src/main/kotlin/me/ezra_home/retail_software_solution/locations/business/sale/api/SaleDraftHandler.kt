package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale.SaleAssembler
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLinesInsertPreparer
import me.ezra_home.retail_software_solution.locations.business.sale.SaleMutator
import me.ezra_home.retail_software_solution.locations.business.sale.SaleValidator
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentCreateDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
@TransactionalOnLocationSchema
class SaleDraftHandler(
    private val saleDataFetcher: SaleDataFetcher,
    private val saleAssembler: SaleAssembler,
    private val saleLinesInsertPreparer: SaleLinesInsertPreparer,
    private val saleMutator: SaleMutator,
    private val contactService: ContactService,
    private val fiscalPeriodService: FiscalPeriodService,
) {

    fun createDraft(dto: SaleCreateDto): SaleResponseDto {
        if (dto.walkInCustomer()) throw RtsGenericException("Walk-in sales cannot be drafted")
        val contactId = dto.resolveContactId()
        contactService.guardExists(contactId)
        guardDatePolicyWhenPaymentsPresent(dto.payments, dto.dateSold)
        val insertContext = saleLinesInsertPreparer.prepareForSaleCreation(dto.linesToAdd)
        val sale = SaleEntity(
            contactId = contactId,
            soldById = dto.soldById,
            dateSold = dto.dateSold,
            notes = dto.notes,
            status = SaleStatus.DRAFT,
        )
        val outcome = saleMutator.create(dto, sale, insertContext)
        return saleAssembler.buildResponse(sale, outcome.lines, insertContext.productSummaries)
    }

    fun updateDraft(dto: SaleUpdateDto): SaleResponseDto {
        SaleValidator.guardNotReassigningToWalkIn(dto)
        val sale = saleDataFetcher.lockAndGetSale(dto.id)
        SaleValidator.guardIsDraft(sale)
        dto.applyTo(sale)
        contactService.guardExists(sale.contactId)
        guardDatePolicyWhenPaymentsPresent(dto.payments, sale.dateSold)
        val outcome = saleMutator.updateAndSyncReservations(dto, sale)
        return saleAssembler.buildResponse(sale, outcome.survivingLines, outcome.productSummaries)
    }

    private fun guardDatePolicyWhenPaymentsPresent(payments: List<SalePaymentCreateDto>, dateSold: OffsetDateTime?) {
        if (payments.isEmpty()) return
        val effective = dateSold ?: DateTimes.Offset.Now.organization()
        SaleValidator.guardDateSoldIsNotFuture(effective)
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(effective))
    }
}
