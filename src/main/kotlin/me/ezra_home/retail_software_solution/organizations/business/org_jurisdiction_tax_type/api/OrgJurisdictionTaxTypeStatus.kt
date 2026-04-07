package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class OrgJurisdictionTaxTypeStatus(override val code: String) : HasCode {
    ACTIVE("A"),
    INACTIVE("I")
}
