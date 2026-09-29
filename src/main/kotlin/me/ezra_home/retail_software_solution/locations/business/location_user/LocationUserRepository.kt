package me.ezra_home.retail_software_solution.locations.business.location_user

import me.ezra_home.retail_software_solution.cross_tier.authority.MembershipSummaryProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LocationUserRepository : JpaRepository<LocationUserEntity, UUID> {

    @Query(
        value = """
            select user_id as userId,
                   count(*) as membershipCount,
                   (count(*) filter (where end_on is null) > 0) as active
            from location_user
            group by user_id
        """,
        nativeQuery = true
    )
    fun findMembershipSummaries(): List<MembershipSummaryProjection>
}
