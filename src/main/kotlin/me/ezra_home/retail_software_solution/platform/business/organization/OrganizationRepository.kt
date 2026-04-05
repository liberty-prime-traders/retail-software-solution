package me.ezra_home.retail_software_solution.platform.business.organization

import me.ezra_home.retail_software_solution.platform.model.OrganizationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface OrganizationRepository: JpaRepository<OrganizationEntity, UUID> {
    fun findOneBySubdomain(subdomain: String): OrganizationEntity?
}
