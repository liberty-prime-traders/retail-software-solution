package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.lock.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.locations.business.sale.SaleAssembler
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLinePreparer
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
    private val saleLinePreparer: SaleLinePreparer,
    private val saleMutator: SaleMutator,
    private val contactService: ContactService,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    fun createDraft(dto: SaleCreateDto): SaleResponseDto {
        if (dto.walkInCustomer) throw RtsGenericException("Walk-in sales cannot be drafted")
        val contactId = dto.resolveContactId().also { contactService.getContactById(it) }
        val validated = saleLinePreparer.prepareForInsert(dto.linesToAdd)
        val sale = SaleEntity(
            contactId = contactId,
            soldBy = dto.soldBy,
            dateSold = dto.dateSold,
            notes = dto.notes,
            status = SaleStatus.DRAFT,
        )
        val outcome = saleMutator.create(dto, sale, validated)
        return saleAssembler.buildResponse(sale, outcome.lines, validated.productSummaries)
    }

    fun updateDraft(dto: SaleUpdateDto): SaleResponseDto {
        entityAdvisoryLock.acquire(LockNamespaces.SALE, listOf(dto.id))
        val sale = saleRepository.getReferenceById(dto.id)
        SaleValidator.guardIsDraft(sale)
        dto.applyTo(sale)
        val outcome = saleMutator.update(dto, sale)
        return saleAssembler.buildResponse(sale, outcome.survivingLines, outcome.productSummaries)
    }
}
