package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.lock.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.locations.business.sale.SaleAssembler
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLinesInsertPreparer
import me.ezra_home.retail_software_solution.locations.business.sale.SaleMutator
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleValidator
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class SaleDraftHandler(
    private val saleRepository: SaleRepository,
    private val saleAssembler: SaleAssembler,
    private val saleLinesInsertPreparer: SaleLinesInsertPreparer,
    private val saleMutator: SaleMutator,
    private val contactService: ContactService,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    fun createDraft(dto: SaleCreateDto): SaleResponseDto {
        if (dto.walkInCustomer) throw RtsGenericException("Walk-in sales cannot be drafted")
        val contactId = dto.resolveContactId()
        contactService.guardExists(contactId)
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
        val sale = saleRepository.getReferenceById(dto.id)
        entityAdvisoryLock.acquire(LockNamespaces.SALE, sale.id!!)
        SaleValidator.guardIsDraft(sale)
        val outcome = saleMutator.updateAndSyncReservations(dto, sale)
        dto.applyTo(sale)
        contactService.guardExists(sale.contactId)
        return saleAssembler.buildResponse(sale, outcome.survivingLines, outcome.productSummaries)
    }
}
