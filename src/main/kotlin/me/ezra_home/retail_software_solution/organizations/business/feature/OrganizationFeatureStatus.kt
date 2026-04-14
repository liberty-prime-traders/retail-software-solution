package me.ezra_home.retail_software_solution.organizations.business.feature

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class OrganizationFeatureStatus(override val code: String) : HasCode {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");
}
