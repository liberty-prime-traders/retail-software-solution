package me.ezra_home.retail_software_solution.organizations.business.tax_rate.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class TaxRateDto(
    var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var orgJurisdictionTaxTypeId: UUID,
    var name: String,
    var ratePercentage: BigDecimal? = null,
    var rateFlatAmount: BigDecimal? = null,
    var startDate: LocalDate,
    var endDate: LocalDate? = null
)
