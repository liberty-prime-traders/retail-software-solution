package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactStatus
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerService
import me.ezra_home.retail_software_solution.organizations.business.org_profile.api.OrgDataSeeder
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import org.springframework.stereotype.Component

@Component
@TransactionalOnOrganizationSchema
class ContactSeeder(
    private val contactCache: ContactCache,
    private val subledgerService: SubledgerService
) : OrgDataSeeder {

    override fun seed() {
        if (contactCache.getAllContacts().none { it.id == SystemContact.WALK_IN.id }) {
            val entity = ContactEntity(
                contactTypes = setOf(ContactType.CUSTOMER),
                firstName = "Walk-In Customer",
                status = ContactStatus.ACTIVE,
                systemDefined = true
            )
            entity.id = SystemContact.WALK_IN.id
            val walkInContact = contactCache.save(entity)
            subledgerService.openBalance(walkInContact.referenceNumber)
        }
    }
}
