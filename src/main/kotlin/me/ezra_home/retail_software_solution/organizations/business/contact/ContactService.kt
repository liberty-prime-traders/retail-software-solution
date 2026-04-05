package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.ContactInsertDto
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.ContactResponseDto
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.ContactUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.IdentityType
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
    fun getAllContacts(): Collection<ContactResponseDto> {
        return contactCache.getAllContacts().map { contactMapper.toResponseDto(it) }
    }

    fun createContact(contactInsertDto: ContactInsertDto): ContactResponseDto {
        contactValidator.validateIdentity(
            contactInsertDto.identityType,
            contactInsertDto.firstName,
            contactInsertDto.companyName
        )
        val dto = contactMapper.toDomainDto(contactInsertDto)
        cleanupIncompatibleFields(dto, contactInsertDto.identityType)
        contactValidator.validateUniqueness(dto.identity)
        contactCache.upsertContact(dto)
        return contactMapper.toResponseDto(dto)
    }

    fun cleanupIncompatibleFields(dto: ContactDto, identityType: IdentityType) {
        when (identityType) {
            IdentityType.ORGANIZATION -> {
                dto.firstName = null
                dto.lastName = null
            }
            IdentityType.INDIVIDUAL -> {
                dto.companyName = null
            }
        }
    }

    fun updateContact(contactUpdateDto: ContactUpdateDto): ContactResponseDto {
        val id = contactUpdateDto.id
        val dto = contactCache.getAllContacts().find { it.id == id }
            ?: throw UpdatingNonExistingRecordException()

        contactMapper.partialUpdate(contactUpdateDto, dto)

        val identityType = contactUpdateDto.identityType
            ?.let { it.orElseGet { determineIdentityType(dto) } }
            ?: determineIdentityType(dto)

        contactValidator.validateIdentity(
            identityType,
            dto.firstName,
            dto.companyName
        )
        cleanupIncompatibleFields(dto, identityType)
        contactValidator.validateUniqueness(dto.identity, id)
        contactCache.upsertContact(dto)
        return contactMapper.toResponseDto(dto)
    }

    private fun determineIdentityType(dto: ContactDto): IdentityType {
        return when (dto.identity) {
            is ContactIdentity.Organization -> IdentityType.ORGANIZATION
            is ContactIdentity.Individual -> IdentityType.INDIVIDUAL
        }
    }

    fun deleteContact(id: UUID) {
        contactCache.deleteContact(id)
    }
}
