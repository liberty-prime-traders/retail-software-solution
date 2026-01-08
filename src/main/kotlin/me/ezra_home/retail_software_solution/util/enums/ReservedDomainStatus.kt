package me.ezra_home.retail_software_solution.util.enums

enum class ReservedDomainStatus(override val code: String) : HasCode {
    USED("USD"),
    UNUSED("UNSD"),
    ABANDONED("ABND");
}
