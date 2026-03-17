package me.ezra_home.retail_software_solution.platform.business.tax_type.dto

import me.ezra_home.retail_software_solution.util.enums.CalculationMethod
import java.io.Serializable

data class TaxTypeInsertDto(
    val name: String,
    val description: String? = null,
    val calculationMethod: CalculationMethod
) : Serializable
