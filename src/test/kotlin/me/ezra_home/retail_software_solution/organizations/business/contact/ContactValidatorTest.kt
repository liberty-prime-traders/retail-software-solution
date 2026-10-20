package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class ContactValidatorTest {

    private val contactValidator = ContactValidator(mock(ContactCache::class.java))

    @Test
    fun `a lender may hold other business types but never other`() {
        contactValidator.validateContactTypes(setOf(ContactType.LENDER))
        contactValidator.validateContactTypes(setOf(ContactType.LENDER, ContactType.SUPPLIER))

        assertThrows(RtsGenericException::class.java) {
            contactValidator.validateContactTypes(setOf(ContactType.LENDER, ContactType.OTHER))
        }
    }
}
