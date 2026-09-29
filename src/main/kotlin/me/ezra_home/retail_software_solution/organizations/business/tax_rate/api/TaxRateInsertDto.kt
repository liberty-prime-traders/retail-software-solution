package me.ezra_home.retail_software_solution.organizations.business.tax_rate.api

import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class TaxRateInsertDto(
    val orgJurisdictionTaxTypeId: UUID,
    val name: String,
    val ratePercentage: BigDecimal? = null,
    val rateFlatAmount: BigDecimal? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null
) : Serializable
