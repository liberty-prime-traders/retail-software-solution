package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.ContactInsertDto
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.ContactResponseDto
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.ContactUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.IdentityType
import me.ezra_home.retail_software_solution.organizations.model.ContactEntity
import me.ezra_home.retail_software_solution.util.exceptions.QueriedByEmptyIdException
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
        val entity = contactMapper.toEntity(contactInsertDto)
        cleanupIncompatibleFields(entity, contactInsertDto.identityType)
        contactValidator.validateUniqueness(entity.identity)
        contactCache.upsertContact(entity)
        return contactMapper.toResponseDto(entity)
    }

    fun cleanupIncompatibleFields(
        entity: ContactEntity,
        identityType: IdentityType
    ) {
        when (identityType) {
            IdentityType.ORGANIZATION -> {
                entity.firstName = null
                entity.lastName = null
            }
            IdentityType.INDIVIDUAL -> {
                entity.companyName = null
            }
        }
    }

    fun updateContact(contactUpdateDto: ContactUpdateDto): ContactResponseDto {
        val id = contactUpdateDto.id ?: throw QueriedByEmptyIdException()
        val entityFromDatabase = contactCache.getAllContacts().find { it.id == id }
            ?: throw UpdatingNonExistingRecordException()

        contactMapper.partialUpdate(contactUpdateDto, entityFromDatabase)

        val identityType = contactUpdateDto.identityType
            ?.let { it.orElseGet { determineIdentityType(entityFromDatabase) } }
            ?: determineIdentityType(entityFromDatabase)

        contactValidator.validateIdentity(
            identityType,
            entityFromDatabase.firstName,
            entityFromDatabase.companyName
        )
        cleanupIncompatibleFields(entityFromDatabase, identityType)
        contactValidator.validateUniqueness(entityFromDatabase.identity, id)
        contactCache.upsertContact(entityFromDatabase)
        return contactMapper.toResponseDto(entityFromDatabase)
    }

    private fun determineIdentityType(entity: ContactEntity): IdentityType {
        return when (entity.identity) {
            is ContactIdentity.Organization -> IdentityType.ORGANIZATION
            is ContactIdentity.Individual -> IdentityType.INDIVIDUAL
        }
    }

    fun deleteContact(id: UUID) {
        contactCache.deleteContact(id)
    }
}
