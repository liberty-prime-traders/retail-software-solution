package me.ezra_home.retail_software_solution.organizations.business.contact.api

import me.ezra_home.retail_software_solution.organizations.business.contact.ContactCache
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactResponseMapperImpl
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactValidator
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerService
import me.ezra_home.retail_software_solution.util.business.mappers.StringQualifier
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class ContactServiceTest {

    private val contactCache = mock(ContactCache::class.java)
    private val subledgerService = mock(SubledgerService::class.java)
    private val userQualifier = mock(UserQualifier::class.java)

    private val contactService = ContactService(
        ContactResponseMapperImpl(userQualifier, StringQualifier),
        contactCache,
        mock(ContactValidator::class.java),
        subledgerService
    )

    @Test
    fun `creating a contact opens its subledger balance and reports it as zero`() {
        val createdById = UUID.randomUUID()
        val contactInsertDto = ContactInsertDto(
            contactTypes = setOf(ContactType.CUSTOMER),
            identityType = IdentityType.INDIVIDUAL,
            firstName = "Ada"
        )
        `when`(contactCache.create(contactInsertDto)).thenReturn(
            ContactDto(
                id = UUID.randomUUID(),
                createdById = createdById,
                createdOn = OffsetDateTime.parse("2026-01-01T10:00:00Z"),
                referenceNumber = "CT000001",
                contactTypes = contactInsertDto.contactTypes,
                firstName = "Ada"
            )
        )
        `when`(userQualifier.getCreatorFullName(createdById)).thenReturn("Test User")

        val contactResponseDto = contactService.createContact(contactInsertDto)

        verify(subledgerService).openBalance("CT000001")
        assertEquals(0, BigDecimal.ZERO.compareTo(contactResponseDto.receivableBalance))
        assertEquals(0, BigDecimal.ZERO.compareTo(contactResponseDto.payableBalance))
    }

    @Test
    fun `reconciling subledger balances covers every contact`() {
        `when`(contactCache.getAllContacts()).thenReturn(listOf(contactDto("CT000001"), contactDto("CT000002")))

        contactService.reconcileSubledgerBalances()

        verify(subledgerService).reconcileBalances(setOf("CT000001", "CT000002"))
    }

    private fun contactDto(referenceNumber: String) = ContactDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.parse("2026-01-01T10:00:00Z"),
        referenceNumber = referenceNumber,
        contactTypes = setOf(ContactType.CUSTOMER),
        firstName = "Ada"
    )
}
