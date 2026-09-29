package me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api

import java.time.OffsetDateTime
import java.util.UUID

data class JurisdictionTaxTypeDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val taxTypeId: UUID,
    val jurisdictionId: UUID,
    val active: Boolean = true
)
