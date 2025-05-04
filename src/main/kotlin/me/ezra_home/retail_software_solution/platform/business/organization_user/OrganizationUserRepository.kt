package me.ezra_home.retail_software_solution.platform.business.organization_user

import me.ezra_home.retail_software_solution.platform.model.OrganizationUserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrganizationUserRepository : JpaRepository<OrganizationUserEntity, UUID> {
    fun existsByOrganizationIdAndUserId(organizationId: UUID, userId: UUID): Boolean
}
