package me.ezra_home.retail_software_solution.organizations.business.contact.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class ContactType(override val code: String) : HasCode {
    CUSTOMER("CUS"),
    SERVICE_PROVIDER("SPR"),
    SUPPLIER("SUP"),
    EMPLOYEE("EMP"),
    GOVERNMENT("GOV"),
    OTHER("OTH")
}
