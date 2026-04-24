package me.ezra_home.retail_software_solution.organizations.business.unitconversion.api

import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal
import java.util.UUID

data class ConversionTargetDto(
    val id: UUID,
    val name: String,
    val code: String,
    val numerator: Long,
    val denominator: Long,
    val factor: BigDecimal = Decimals.divideScale4(numerator.toBigDecimal(), denominator.toBigDecimal())
)
