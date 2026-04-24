package me.ezra_home.retail_software_solution.organizations.business.contact.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactEntity
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactRepository
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import org.springframework.stereotype.Component

@Component
@TransactionalOnOrganizationSchema
class WalkInContactSeeder(private val contactRepository: ContactRepository) {

    fun seed() {
        if (contactRepository.existsById(SystemContact.WALK_IN.id)) return
        val entity = ContactEntity(
            contactType = ContactType.CUSTOMER,
            companyName = "Walk-In Customer",
            status = ContactStatus.ACTIVE
        )
        entity.id = SystemContact.WALK_IN.id
        contactRepository.save(entity)
    }
}
