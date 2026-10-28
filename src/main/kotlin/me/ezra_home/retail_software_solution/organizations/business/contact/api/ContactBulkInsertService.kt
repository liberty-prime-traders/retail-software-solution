package me.ezra_home.retail_software_solution.organizations.business.contact.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactCache
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactResponseMapper
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactValidator
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerBalanceDto
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerService
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema
class ContactBulkInsertService(
    private val contactValidator: ContactValidator,
    private val contactCache: ContactCache,
    private val contactResponseMapper: ContactResponseMapper,
    private val subledgerService: SubledgerService
) {

    fun bulkCreateContacts(contactBulkInsertDtos: List<ContactBulkInsertDto>): List<ContactResponseDto> {
        val errors = mutableListOf<String>()
        val validatedInsertDtos = mutableListOf<ContactInsertDto>()
        val identityKeysInRequest = mutableSetOf<String>()
        contactBulkInsertDtos.forEachIndexed { index, contactBulkInsertDto ->
            try {
                val contactInsertDto = toValidatedInsertDto(contactBulkInsertDto)
                contactValidator.validateUniqueness(contactInsertDto.identity)
                val identityKey = contactInsertDto.identity.normalizedKey
                if (identityKey.isNotBlank() && !identityKeysInRequest.add(identityKey)) {
                    throw RtsGenericException("A Contact with ${contactInsertDto.identity.description()} appears more than once in the request")
                }
                validatedInsertDtos.add(contactInsertDto)
            } catch (rtsGenericException: RtsGenericException) {
                errors.add("Row ${index + 1}: ${rtsGenericException.message}")
            }
        }
        if (errors.isNotEmpty()) {
            throw RtsGenericException("Bulk contact import validation failed", errors)
        }
        val createdContactDtos = contactCache.createAll(validatedInsertDtos)
        subledgerService.openBalances(createdContactDtos.map { it.referenceNumber })
        return createdContactDtos.map {
            contactResponseMapper.toResponseDto(it, SubledgerBalanceDto.ZERO, SubledgerBalanceDto.ZERO)
        }
    }

    private fun toValidatedInsertDto(contactBulkInsertDto: ContactBulkInsertDto): ContactInsertDto {
        val contactTypes = parseContactTypes(contactBulkInsertDto.contactTypes)
        contactValidator.validateContactTypes(contactTypes)
        val identityType = if (StringUtils.hasValue(contactBulkInsertDto.companyName)) {
            IdentityType.ORGANIZATION
        } else {
            IdentityType.INDIVIDUAL
        }
        contactValidator.validateIdentity(identityType, contactBulkInsertDto.firstName, contactBulkInsertDto.companyName)
        val isOrganization = identityType == IdentityType.ORGANIZATION
        return ContactInsertDto(
            contactTypes = contactTypes,
            identityType = identityType,
            firstName = if (isOrganization) null else StringUtils.getValueOrNull(contactBulkInsertDto.firstName),
            lastName = if (isOrganization) null else StringUtils.getValueOrNull(contactBulkInsertDto.lastName),
            companyName = StringUtils.getValueOrNull(contactBulkInsertDto.companyName),
            email = StringUtils.getValueOrNull(contactBulkInsertDto.email),
            phone = StringUtils.getValueOrNull(contactBulkInsertDto.phone),
            address = StringUtils.getValueOrNull(contactBulkInsertDto.address),
            creditLimit = contactBulkInsertDto.creditLimit,
            notes = StringUtils.getValueOrNull(contactBulkInsertDto.notes)
        )
    }

    private fun parseContactTypes(commaSeparatedContactTypeCodes: String?): Set<ContactType> {
        val contactTypesByCode = ContactType.entries.associateBy { it.code }
        return commaSeparatedContactTypeCodes.orEmpty()
            .split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .map { code ->
                contactTypesByCode[code] ?: throw RtsGenericException("Unknown contact type '$code'")
            }
            .toSet()
    }
}
