package me.ezra_home.retail_software_solution.cross_tier.authority

import me.ezra_home.retail_software_solution.util.enums.SchemaLevel

enum class AuthorityType { ROLE, PERMISSION }

data class Authority(
    val name: String,
    val tier: SchemaLevel,
    val type: AuthorityType,
    val holderCount: Int?
)
