package me.ezra_home.retail_software_solution.organizations.business.tax_rate.api

import java.math.BigDecimal

data class ActiveTaxRateDto(
    val ratePercentage: BigDecimal?,
    val rateFlatAmount: BigDecimal?
)
