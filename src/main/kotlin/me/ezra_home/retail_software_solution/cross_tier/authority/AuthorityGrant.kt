package me.ezra_home.retail_software_solution.cross_tier.authority

import java.time.OffsetDateTime
import java.util.UUID

data class AuthorityGrant(
    val userId: UUID,
    val grantedAt: OffsetDateTime
)
