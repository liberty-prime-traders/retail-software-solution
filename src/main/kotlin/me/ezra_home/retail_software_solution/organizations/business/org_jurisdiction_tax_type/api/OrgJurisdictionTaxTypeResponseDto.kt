package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api

import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxRecoveryType
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class OrgJurisdictionTaxTypeResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val createdOn: OffsetDateTime?,
    val platformTaxId: UUID,
    val taxLabel: String,
    val taxRecoveryType: TaxRecoveryType,
    val status: OrgJurisdictionTaxTypeStatus,
    val payableAccountCode: String?,
    val payableAccount: String?,
    val recoverableAccountCode: String?,
    val recoverableAccount: String?,
    val taxInclusive: Boolean
) : Serializable
