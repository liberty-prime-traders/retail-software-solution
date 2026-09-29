package me.ezra_home.retail_software_solution.organizations.business.tax_rate

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class TaxRateDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val orgJurisdictionTaxTypeId: UUID,
    val name: String,
    val ratePercentage: BigDecimal? = null,
    val rateFlatAmount: BigDecimal? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null
)
