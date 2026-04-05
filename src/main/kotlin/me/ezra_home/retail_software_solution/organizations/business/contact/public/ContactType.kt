package me.ezra_home.retail_software_solution.organizations.business.contact.public

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class ContactType(override val code: String) : HasCode {
    CUSTOMER("CUS"),
    CONTRACTOR("CTR"),
    SUPPLIER("SUP"),
    EMPLOYEE("EMP"),
    OTHER("OTH")
}
