package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.dto

import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.public.OrgJurisdictionTaxTypeStatus
import java.time.OffsetDateTime
import java.util.UUID

data class OrgJurisdictionTaxTypeDto(
    var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var jurisdictionTaxTypeId: UUID? = null,
    var status: OrgJurisdictionTaxTypeStatus = OrgJurisdictionTaxTypeStatus.ACTIVE
)
