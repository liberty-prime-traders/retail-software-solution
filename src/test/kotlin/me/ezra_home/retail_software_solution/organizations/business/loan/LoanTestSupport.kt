package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import org.mockito.ArgumentMatchers.any
import java.time.OffsetDateTime
import java.util.UUID

object LoanTestSupport {

    // Mockito's any() returns null, which Kotlin rejects for non-null parameters.
    @Suppress("UNCHECKED_CAST")
    fun <T> anyNonNull(type: Class<T>): T {
        any(type)
        return null as T
    }

    fun stamp(entity: HasReferenceEntity, reference: String) {
        entity.id = UUID.randomUUID()
        entity.referenceNumber = reference
        entity.createdById = UUID.randomUUID()
        entity.createdOn = OffsetDateTime.now()
    }

    fun lenderContactDto() = ContactDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = "CT000001",
        contactTypes = setOf(ContactType.LENDER),
        companyName = "First Bank"
    )
}
