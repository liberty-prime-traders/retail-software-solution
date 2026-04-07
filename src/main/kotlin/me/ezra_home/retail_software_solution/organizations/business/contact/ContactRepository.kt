package me.ezra_home.retail_software_solution.organizations.business.contact

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ContactRepository: JpaRepository<ContactEntity, UUID>
