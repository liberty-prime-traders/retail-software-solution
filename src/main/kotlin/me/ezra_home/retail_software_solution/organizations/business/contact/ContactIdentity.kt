package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.util.business.StringUtils

sealed interface ContactIdentity {
    val rawValue: String

    val normalizedKey: String
        get() = StringUtils.normalizeForComparison(rawValue)

    fun displayName(): String = when (this) {
        is Organization -> "company '$rawValue'"
        is Individual -> "person '$rawValue'"
    }

    data class Organization(val name: String) : ContactIdentity {
        init {
            require(name.isNotBlank()) { "Company name is required for organizations" }
        }
        override val rawValue = name
    }

    data class Individual(val firstName: String, val lastName: String?) : ContactIdentity {
        init {
            require(firstName.isNotBlank()) { "First name is required" }
        }
        override val rawValue = "$firstName ${lastName.orEmpty()}".trim()
    }
}
