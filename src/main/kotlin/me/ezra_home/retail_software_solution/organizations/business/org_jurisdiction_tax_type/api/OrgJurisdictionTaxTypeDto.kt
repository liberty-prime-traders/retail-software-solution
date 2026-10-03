package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api

import java.time.OffsetDateTime
import java.util.UUID

data class OrgJurisdictionTaxTypeDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val jurisdictionTaxTypeId: UUID,
    val status: OrgJurisdictionTaxTypeStatus = OrgJurisdictionTaxTypeStatus.ACTIVE,
    val payableAccountCode: String,
    val recoverableAccountCode: String? = null
)
