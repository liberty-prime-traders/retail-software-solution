package me.ezra_home.retail_software_solution.cross_tier.authority

import java.util.UUID

data class MembershipSummaryRow(
    val userId: UUID,
    val membershipCount: Long,
    val active: Boolean
)
