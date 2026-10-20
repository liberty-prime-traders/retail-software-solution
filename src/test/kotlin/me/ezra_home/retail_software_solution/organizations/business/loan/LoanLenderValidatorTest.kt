package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactStatus
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.UUID

class LoanLenderValidatorTest {

    private val contactService = mock(ContactService::class.java)
    private val loanLenderValidator = LoanLenderValidator(contactService)

    @Test
    fun `an active contact holding the lender type is accepted`() {
        val lender = contact("CT000001", setOf(ContactType.SUPPLIER, ContactType.LENDER))
        `when`(contactService.findContactByReferenceNumber("CT000001")).thenReturn(lender)

        assertEquals(lender, loanLenderValidator.requireActiveLender("CT000001"))
    }

    @Test
    fun `a blank or unknown lender reference is rejected`() {
        `when`(contactService.findContactByReferenceNumber("CT000001")).thenReturn(contact("CT000001", setOf(ContactType.LENDER)))

        listOf(null, "", "  ", "CT999999").forEach { reference ->
            assertThrows(RtsGenericException::class.java) { loanLenderValidator.requireActiveLender(reference) }
        }
    }

    @Test
    fun `a suspended or inactive lender is rejected`() {
        listOf(ContactStatus.SUSPENDED, ContactStatus.INACTIVE).forEach { status ->
            `when`(contactService.findContactByReferenceNumber("CT000001")).thenReturn(contact("CT000001", setOf(ContactType.LENDER), status))

            assertThrows(RtsGenericException::class.java) { loanLenderValidator.requireActiveLender("CT000001") }
        }
    }

    @Test
    fun `an active contact without the lender type is rejected`() {
        `when`(contactService.findContactByReferenceNumber("CT000001")).thenReturn(contact("CT000001", setOf(ContactType.SUPPLIER, ContactType.CUSTOMER)))

        assertThrows(RtsGenericException::class.java) { loanLenderValidator.requireActiveLender("CT000001") }
    }

    private fun contact(referenceNumber: String, contactTypes: Set<ContactType>, status: ContactStatus = ContactStatus.ACTIVE) = ContactDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = referenceNumber,
        contactTypes = contactTypes,
        companyName = "First Bank",
        status = status
    )
}
