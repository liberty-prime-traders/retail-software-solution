package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class EntryType(override val code: String) : HasCode {
    DEBIT("DEBIT"),
    CREDIT("CREDIT");
}
