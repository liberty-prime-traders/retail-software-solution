package me.ezra_home.retail_software_solution.util.enums

enum class ContactType(override val code: String) : HasCode {
    CUSTOMER("C"),
    VENDOR("V"),
    SUPPLIER("S")
}
