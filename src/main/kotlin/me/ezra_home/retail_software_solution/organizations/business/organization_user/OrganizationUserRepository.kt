package me.ezra_home.retail_software_solution.organizations.business.organization_user

import me.ezra_home.retail_software_solution.cross_tier.authority.MembershipSummaryProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrganizationUserRepository : JpaRepository<OrganizationUserEntity, UUID> {

    @Query(
        value = """
            select user_id as userId,
                   count(*) as membershipCount,
                   (count(*) filter (where end_on is null) > 0) as active
            from organization_user
            group by user_id
        """,
        nativeQuery = true
    )
    fun findMembershipSummaries(): List<MembershipSummaryProjection>
}
