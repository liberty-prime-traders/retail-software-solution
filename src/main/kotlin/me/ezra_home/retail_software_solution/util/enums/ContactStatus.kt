package me.ezra_home.retail_software_solution.util.enums

enum class ContactStatus(override val code: String) : HasCode {
    ACTIVE("A"),
    SUSPENDED("XS"),
    INACTIVE("X")
}
