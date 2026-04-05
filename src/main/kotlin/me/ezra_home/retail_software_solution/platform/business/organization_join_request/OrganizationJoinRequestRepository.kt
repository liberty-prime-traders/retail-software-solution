package me.ezra_home.retail_software_solution.platform.business.organization_join_request

import me.ezra_home.retail_software_solution.platform.business.organization_join_request.OrganizationJoinRequestEntity
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.`public`.JoinRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrganizationJoinRequestRepository : JpaRepository<OrganizationJoinRequestEntity, UUID> {

    fun existsBySubdomainAndCreatedByIdAndStatus(
        subdomain: String,
        createdById: UUID,
        status: JoinRequestStatus
    ): Boolean

    fun findAllByCreatedById(createdById: UUID): Collection<OrganizationJoinRequestEntity>

    fun findByOrganizationId(organizationId: UUID): Collection<OrganizationJoinRequestEntity>

}
