package me.ezra_home.retail_software_solution.platform.business.organization

import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrganizationRepository: JpaRepository<OrganizationEntity, UUID> {
    fun findOneBySubdomain(subdomain: String): OrganizationEntity?
}
