package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerService
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.UUID

class ContactSeederTest {

    private val contactCache = mock(ContactCache::class.java)
    private val contactService = mock(ContactService::class.java)
    private val subledgerService = mock(SubledgerService::class.java)
    private val contactSeeder = ContactSeeder(contactCache, contactService, subledgerService)

    private val walkInContact = contactDto(SystemContact.WALK_IN.id, "CT000009", "Walk-In Customer")

    @Test
    fun `seeding a new organization creates the walk-in contact and opens its subledger balance`() {
        `when`(contactCache.getAllContacts()).thenReturn(emptyList())
        val placeholderEntity = mock(ContactEntity::class.java)
        `when`(contactCache.save(any(ContactEntity::class.java) ?: placeholderEntity)).thenReturn(walkInContact)

        contactSeeder.seed()

        assertTrue(savedContactEntities())
        verify(subledgerService).openBalance(walkInContact.referenceNumber)
        verify(contactService).reconcileSubledgerBalances()
    }

    @Test
    fun `re-seeding an existing organization leaves its contacts alone and reconciles balances`() {
        `when`(contactCache.getAllContacts()).thenReturn(listOf(walkInContact))

        contactSeeder.seed()

        assertFalse(savedContactEntities())
        verifyNoInteractions(subledgerService)
        verify(contactService).reconcileSubledgerBalances()
    }

    private fun savedContactEntities() =
        mockingDetails(contactCache).invocations.any { it.method.name == "save" }

    private fun contactDto(id: UUID, referenceNumber: String, firstName: String) = ContactDto(
        id = id,
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.parse("2026-01-01T10:00:00Z"),
        referenceNumber = referenceNumber,
        contactTypes = setOf(ContactType.CUSTOMER),
        firstName = firstName
    )
}
