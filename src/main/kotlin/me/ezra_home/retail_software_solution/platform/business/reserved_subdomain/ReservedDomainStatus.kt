package me.ezra_home.retail_software_solution.platform.business.reserved_subdomain

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class ReservedDomainStatus(override val code: String) : HasCode {
    USED("USD"),
    UNUSED("UNSD"),
    ABANDONED("ABND");
}
