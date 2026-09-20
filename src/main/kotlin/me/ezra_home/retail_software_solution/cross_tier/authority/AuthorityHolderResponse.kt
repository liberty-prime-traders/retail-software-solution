package me.ezra_home.retail_software_solution.cross_tier.authority

import java.time.OffsetDateTime

data class AuthorityHolderResponse(
    val fullName: String?,
    val grantedOn: OffsetDateTime,
    val authorityName: String
)
