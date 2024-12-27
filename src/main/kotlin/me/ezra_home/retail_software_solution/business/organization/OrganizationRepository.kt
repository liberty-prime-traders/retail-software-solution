package me.ezra_home.retail_software_solution.business.organization

import me.ezra_home.retail_software_solution.model.entity.OrganizationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrganizationRepository: JpaRepository<OrganizationEntity, UUID>
