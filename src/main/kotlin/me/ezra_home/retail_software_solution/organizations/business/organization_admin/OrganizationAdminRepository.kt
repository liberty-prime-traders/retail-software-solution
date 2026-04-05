package me.ezra_home.retail_software_solution.organizations.business.organization_admin

import me.ezra_home.retail_software_solution.organizations.model.OrganizationAdminEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface OrganizationAdminRepository: JpaRepository<OrganizationAdminEntity, UUID>
