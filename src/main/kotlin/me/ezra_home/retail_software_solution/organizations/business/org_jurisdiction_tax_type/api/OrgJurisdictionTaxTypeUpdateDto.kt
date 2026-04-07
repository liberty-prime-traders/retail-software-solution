package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api

import java.io.Serializable
import java.util.UUID

data class OrgJurisdictionTaxTypeUpdateDto(
    val id: UUID,
    val status: OrgJurisdictionTaxTypeStatus
) : Serializable {

    fun applyTo(existing: OrgJurisdictionTaxTypeDto): OrgJurisdictionTaxTypeDto = existing.copy(
        status = status
    )
}
