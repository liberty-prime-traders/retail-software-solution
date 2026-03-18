package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.dto

import java.io.Serializable
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class OrgJurisdictionTaxTypeResponseDto(
    val id: UUID?,
    val referenceNumber: String?,
    val createdOn: OffsetDateTime?,
    val jurisdictionTaxType: String,
    val startDate: LocalDate,
    val endDate: LocalDate?
) : Serializable
