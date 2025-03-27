package me.ezra_home.retail_software_solution.platform.business.organizationadmin

import me.ezra_home.retail_software_solution.platform.model.OrganizationAdminEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrganizationAdminRepository: JpaRepository<OrganizationAdminEntity, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<OrganizationAdminEntity>
}
