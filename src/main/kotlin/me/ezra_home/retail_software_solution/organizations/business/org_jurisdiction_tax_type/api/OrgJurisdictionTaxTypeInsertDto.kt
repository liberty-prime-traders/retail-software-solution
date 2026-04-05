package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api

import java.io.Serializable
import java.util.UUID

data class OrgJurisdictionTaxTypeInsertDto(
    val jurisdictionTaxTypeId: UUID,
    val status: OrgJurisdictionTaxTypeStatus = OrgJurisdictionTaxTypeStatus.ACTIVE
) : Serializable
