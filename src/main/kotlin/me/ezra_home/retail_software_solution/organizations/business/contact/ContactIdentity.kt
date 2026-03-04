package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.util.business.StringUtils

sealed interface ContactIdentity {
    val displayName: String

    val normalizedKey: String
        get() = StringUtils.normalizeForComparison(displayName)

    fun description(): String = when (this) {
        is Organization -> "company '$displayName'"
        is Individual -> "person '$displayName'"
    }

    data class Organization(val name: String) : ContactIdentity {
        init {
            require(name.isNotBlank()) { "Company name is required for organizations" }
        }
        override val displayName = name
    }

    data class Individual(val firstName: String, val lastName: String?) : ContactIdentity {
        init {
            require(firstName.isNotBlank()) { "First name is required" }
        }
        override val displayName = "$firstName ${lastName.orEmpty()}".trim()
    }
}
