package me.ezra_home.retail_software_solution.cross_tier.authority

import java.util.UUID

data class MembershipUserSummary(
    val userId: UUID,
    val fullName: String?,
    val email: String?,
    val membershipCount: Long,
    val active: Boolean
)
