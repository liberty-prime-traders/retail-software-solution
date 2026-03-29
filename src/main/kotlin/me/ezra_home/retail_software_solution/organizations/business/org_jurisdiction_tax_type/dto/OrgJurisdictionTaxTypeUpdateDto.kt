package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.dto

import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.OrgJurisdictionTaxTypeStatus
import java.io.Serializable
import java.util.UUID

data class OrgJurisdictionTaxTypeUpdateDto(
    val id: UUID,
    val status: OrgJurisdictionTaxTypeStatus
) : Serializable
