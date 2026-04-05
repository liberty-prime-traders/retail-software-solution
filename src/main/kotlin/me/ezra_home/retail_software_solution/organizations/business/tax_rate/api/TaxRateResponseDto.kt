package me.ezra_home.retail_software_solution.organizations.business.tax_rate.api

import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class TaxRateResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val createdOn: OffsetDateTime?,
    val orgJurisdictionTaxTypeId: UUID,
    val taxLabel: String?,
    val name: String,
    val ratePercentage: BigDecimal?,
    val rateFlatAmount: BigDecimal?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val parentIsActive: Boolean
) : Serializable
