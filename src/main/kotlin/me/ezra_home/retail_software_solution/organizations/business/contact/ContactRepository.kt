package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.organizations.model.ContactEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface ContactRepository: JpaRepository<ContactEntity, UUID>
