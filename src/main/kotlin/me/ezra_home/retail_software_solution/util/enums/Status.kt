package me.ezra_home.retail_software_solution.util.enums

enum class Status(override val code: String) : HasCode {
    ACTIVE("A"),
    STOPPED("S"),

    USED("USD"),
    UNUSED("UNSD"),
    ABANDONED("ABND");
}
