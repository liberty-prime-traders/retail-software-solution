package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class ContactSubledgerDirection(override val code: String) : HasCode {
    RECEIVABLE("R"),
    PAYABLE("P")
}
