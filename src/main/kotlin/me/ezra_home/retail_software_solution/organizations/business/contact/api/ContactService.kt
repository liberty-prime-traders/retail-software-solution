package me.ezra_home.retail_software_solution.organizations.business.contact.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactCache
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactMapper
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactValidator
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ContactService(
    private val contactMapper: ContactMapper,
    private val contactCache: ContactCache,
    private val contactValidator: ContactValidator
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllContactDtos(): Collection<ContactDto> = contactCache.getAllContacts()

    @TransactionalOnOrganizationSchema(readOnly = true)
        fun getContactById(id: UUID): ContactDto {
        return contactCache.getAllContacts().find { it.id == id }
            ?: throw UpdatingNonExistingRecordException()
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllContacts(): Collection<ContactResponseDto> {
        return contactCache.getAllContacts().map { contactMapper.toResponseDto(it) }
    }

    fun createContact(contactInsertDto: ContactInsertDto): ContactResponseDto {
        contactValidator.validateIdentity(
            contactInsertDto.identityType,
            contactInsertDto.firstName,
            contactInsertDto.companyName
        )
        val cleanedInsert = cleanupIncompatibleFields(contactInsertDto)
        contactValidator.validateUniqueness(cleanedInsert.identity)
        val dto = contactCache.create(cleanedInsert)
        return contactMapper.toResponseDto(dto)
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

        val identityType = contactUpdateDto.identityType
            ?.let { it.orElseGet { determineIdentityType(updated) } }
            ?: determineIdentityType(updated)

        contactValidator.validateIdentity(
            identityType,
            updated.firstName,
            updated.companyName
        )
        updated = cleanupIncompatibleFields(updated, identityType)
        contactValidator.validateUniqueness(updated.identity, id)
        val saved = contactCache.save(updated)
        return contactMapper.toResponseDto(saved)
    }

    private fun determineIdentityType(dto: ContactDto): IdentityType {
        return when (dto.identity) {
            is ContactIdentity.Organization -> IdentityType.ORGANIZATION
            is ContactIdentity.Individual -> IdentityType.INDIVIDUAL
        }
    }

    fun deleteContact(id: UUID) {
        val existing = contactCache.getAllContacts().find { it.id == id } ?: return
        if (existing.systemDefined) throw RtsGenericException("System-defined contacts cannot be deleted")
        contactCache.deleteContact(id)
    }
}
