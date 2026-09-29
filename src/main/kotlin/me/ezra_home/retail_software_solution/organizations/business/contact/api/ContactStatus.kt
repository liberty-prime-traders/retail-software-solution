package me.ezra_home.retail_software_solution.organizations.business.contact.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class ContactStatus(override val code: String) : HasCode {
    ACTIVE("A"),
    SUSPENDED("XS"),
    INACTIVE("X")
}
