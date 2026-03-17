package me.ezra_home.retail_software_solution.util.enums

enum class ContactType(override val code: String) : HasCode {
    CUSTOMER("CUS"),
    CONTRACTOR("CTR"),
    SUPPLIER("SUP"),
    EMPLOYEE("EMP"),
    OTHER("OTH")
}
