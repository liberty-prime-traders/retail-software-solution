package me.ezra_home.retail_software_solution.organizations.business.contact.api

import me.ezra_home.retail_software_solution.organizations.business.contact.ContactCache
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactResponseMapper
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactValidator
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import java.time.OffsetDateTime
import java.util.UUID

class ContactBulkInsertServiceTest {

    private val contactCache = mock(ContactCache::class.java)
    private val subledgerService = mock(SubledgerService::class.java)
    private val contactBulkInsertService = ContactBulkInsertService(
        ContactValidator(contactCache),
        contactCache,
        mock(ContactResponseMapper::class.java),
        subledgerService
    )

    @Test
    fun `reports every failing row together with its row number and creates nothing`() {
        `when`(contactCache.getAllContacts()).thenReturn(emptyList())

        val exception = assertThrows(RtsGenericException::class.java) {
            contactBulkInsertService.bulkCreateContacts(
                listOf(
                    ContactBulkInsertDto(contactTypes = "CUS", firstName = "Ann"),
                    ContactBulkInsertDto(contactTypes = "NOPE", firstName = "Bob"),
                    ContactBulkInsertDto(contactTypes = "CUS")
                )
            )
        }

        assertEquals(
            listOf(
                "Row 2: Unknown contact type 'NOPE'",
                "Row 3: Identity type INDIVIDUAL requires first name"
            ),
            exception.payload
        )
        verify(contactCache, never()).createAll(anyList())
        verifyNoInteractions(subledgerService)
    }

    @Test
    fun `rejects a row with no contact types`() {
        `when`(contactCache.getAllContacts()).thenReturn(emptyList())

        val exception = assertThrows(RtsGenericException::class.java) {
            contactBulkInsertService.bulkCreateContacts(listOf(ContactBulkInsertDto(contactTypes = " , ", firstName = "Ann")))
        }

        assertEquals(listOf("Row 1: At least one contact type required"), exception.payload)
    }

    @Test
    fun `rejects an identity that appears twice in the request`() {
        `when`(contactCache.getAllContacts()).thenReturn(emptyList())

        val exception = assertThrows(RtsGenericException::class.java) {
            contactBulkInsertService.bulkCreateContacts(
                listOf(
                    ContactBulkInsertDto(contactTypes = "CUS", companyName = "Acme Ltd"),
                    ContactBulkInsertDto(contactTypes = "SUP", companyName = "  acme   ltd ")
                )
            )
        }

        val rowMessage = rowMessages(exception).single() as String
        assertTrue(rowMessage.startsWith("Row 2: A Contact with "))
        assertTrue(rowMessage.endsWith("appears more than once in the request"))
    }

    @Test
    fun `rejects an identity that already exists`() {
        `when`(contactCache.getAllContacts()).thenReturn(listOf(existingContact(companyName = "Acme Ltd")))

        val exception = assertThrows(RtsGenericException::class.java) {
            contactBulkInsertService.bulkCreateContacts(listOf(ContactBulkInsertDto(contactTypes = "CUS", companyName = "Acme Ltd")))
        }

        val rowMessage = rowMessages(exception).single() as String
        assertTrue(rowMessage.startsWith("Row 1: A Contact with "))
        assertTrue(rowMessage.endsWith("already exists"))
    }

    @Test
    fun `parses type codes case-insensitively and opens one balance row per created contact`() {
        `when`(contactCache.getAllContacts()).thenReturn(emptyList())
        val expectedInsertDto = ContactInsertDto(
            contactTypes = setOf(ContactType.CUSTOMER, ContactType.SUPPLIER),
            identityType = IdentityType.INDIVIDUAL,
            firstName = "Ann"
        )
        val created = existingContact(firstName = "Ann", referenceNumber = "CON000007")
        `when`(contactCache.createAll(listOf(expectedInsertDto))).thenReturn(listOf(created))

        contactBulkInsertService.bulkCreateContacts(listOf(ContactBulkInsertDto(contactTypes = "cus, sup", firstName = "Ann")))

        verify(subledgerService).openBalances(listOf("CON000007"))
    }

    private fun rowMessages(exception: RtsGenericException): List<*> = exception.payload as List<*>

    private fun existingContact(
        companyName: String? = null,
        firstName: String? = null,
        referenceNumber: String = "CON000001"
    ) = ContactDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
        referenceNumber = referenceNumber,
        contactTypes = setOf(ContactType.CUSTOMER),
        firstName = firstName,
        companyName = companyName
    )
}
