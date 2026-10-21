package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactStatus
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class LoanLenderValidator(
    private val contactService: ContactService
) {

    fun requireActiveLender(lenderContactReference: String?): ContactDto {
        val referenceNumber = StringUtils.getValueOrException(lenderContactReference, "Lender is required")
        val lenderContactDto = contactService.findContactByReferenceNumber(referenceNumber)
            ?: throw RtsGenericException("Lender not found: $referenceNumber")
        val displayName = lenderContactDto.identity.displayName
        if (lenderContactDto.status != ContactStatus.ACTIVE) {
            throw RtsGenericException("Lender $displayName is not active")
        }
        if (ContactType.LENDER !in lenderContactDto.contactTypes) {
            throw RtsGenericException("Contact $displayName is not a lender")
        }
        return lenderContactDto
    }
}
