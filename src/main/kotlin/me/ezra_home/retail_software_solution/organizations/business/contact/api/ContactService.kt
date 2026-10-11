package me.ezra_home.retail_software_solution.organizations.business.contact.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactCache
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactResponseMapper
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactValidator
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactOpeningBalanceReader
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerBalanceDto
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryResponse
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ContactService(
    private val contactResponseMapper: ContactResponseMapper,
    private val contactCache: ContactCache,
    private val contactValidator: ContactValidator,
    private val subledgerService: SubledgerService,
    private val contactOpeningBalanceReader: ContactOpeningBalanceReader
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllContactDtos(): Collection<ContactDto> = contactCache.getAllContacts()

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getContactById(id: UUID): ContactDto {
        return contactCache.getAllContacts().find { it.id == id }
            ?: throw UpdatingNonExistingRecordException()
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun guardExists(id: UUID) {
        if (contactCache.getAllContacts().none { it.id == id }) {
            throw UpdatingNonExistingRecordException()
        }
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllContacts(): Collection<ContactResponseDto> {
        return toResponseDtos(contactCache.getAllContacts())
    }

    fun reconcileSubledgerBalances() {
        subledgerService.reconcileBalances(contactCache.getAllContacts().map { it.referenceNumber }.toSet())
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getLedger(
        id: UUID,
        pageRequest: PageRequest<ContactLedgerSearchParameters, String>
    ): PageResponse<SubledgerEntryResponse, String> {
        val contactDto = getContactById(id)
        return subledgerService.getEntries(contactDto.referenceNumber, pageRequest.previousCursor, pageRequest.requestedSize)
    }

    private fun toResponseDtos(contactDtos: Collection<ContactDto>): List<ContactResponseDto> {
        val balancesByContactReferenceNumber = subledgerService.getCurrentBalances(
            contactDtos.map { it.referenceNumber }.toSet()
        )
        val openingBalancesByContactReferenceNumber = contactOpeningBalanceReader.getOpeningBalances(
            contactDtos.map { it.referenceNumber }.toSet()
        )
        return contactDtos.map { contactDto ->
            val subledgerBalanceDto = balancesByContactReferenceNumber[contactDto.referenceNumber]
                ?: SubledgerBalanceDto.ZERO
            val openingBalanceDto = openingBalancesByContactReferenceNumber[contactDto.referenceNumber]
                ?: SubledgerBalanceDto.ZERO
            contactResponseMapper.toResponseDto(contactDto, subledgerBalanceDto, openingBalanceDto)
        }
    }

    fun createContact(contactInsertDto: ContactInsertDto): ContactResponseDto {
        contactValidator.validateContactTypes(contactInsertDto.contactTypes)
        contactValidator.validateIdentity(
            contactInsertDto.identityType,
            contactInsertDto.firstName,
            contactInsertDto.companyName
        )
        val cleanedInsert = cleanupIncompatibleFields(contactInsertDto)
        contactValidator.validateUniqueness(cleanedInsert.identity)
        val dto = contactCache.create(cleanedInsert)
        subledgerService.openBalance(dto.referenceNumber)
        return contactResponseMapper.toResponseDto(dto, SubledgerBalanceDto.ZERO, SubledgerBalanceDto.ZERO)
    }

    private fun cleanupIncompatibleFields(insertDto: ContactInsertDto): ContactInsertDto {
        return when (insertDto.identityType) {
            IdentityType.ORGANIZATION -> insertDto.copy(firstName = null, lastName = null)
            IdentityType.INDIVIDUAL -> insertDto.copy(companyName = null)
        }
    }

    private fun cleanupIncompatibleFields(dto: ContactDto, identityType: IdentityType): ContactDto {
        return when (identityType) {
            IdentityType.ORGANIZATION -> dto.copy(firstName = null, lastName = null)
            IdentityType.INDIVIDUAL -> dto.copy(companyName = null)
        }
    }

    fun updateContact(contactUpdateDto: ContactUpdateDto): ContactResponseDto {
        val id = contactUpdateDto.id
        val existing = contactCache.getAllContacts().find { it.id == id }
            ?: throw UpdatingNonExistingRecordException()
        if (existing.systemDefined) throw RtsGenericException("System-defined contacts cannot be modified")

        var updated = contactUpdateDto.applyTo(existing)
        contactValidator.validateContactTypes(updated.contactTypes)

        val identityType = contactUpdateDto.identityType
            ?.let { it.orElseGet { updated.identity.identityType } }
            ?: updated.identity.identityType

        contactValidator.validateIdentity(
            identityType,
            updated.firstName,
            updated.companyName
        )
        updated = cleanupIncompatibleFields(updated, identityType)
        contactValidator.validateUniqueness(updated.identity, id)
        val saved = contactCache.save(updated)
        return toResponseDtos(listOf(saved)).single()
    }
}
