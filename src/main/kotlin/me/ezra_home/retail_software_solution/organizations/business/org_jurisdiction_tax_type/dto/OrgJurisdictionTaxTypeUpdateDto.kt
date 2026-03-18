package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.dto

import java.io.Serializable
import java.time.LocalDate
import java.util.UUID

data class OrgJurisdictionTaxTypeUpdateDto(
    val id: UUID,
    val endDate: LocalDate
) : Serializable
