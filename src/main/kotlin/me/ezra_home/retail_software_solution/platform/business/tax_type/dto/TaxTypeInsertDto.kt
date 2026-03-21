package me.ezra_home.retail_software_solution.platform.business.tax_type.dto

import me.ezra_home.retail_software_solution.platform.business.tax_type.CalculationMethod
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxApplicationLevel
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxRecoveryType
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxTrigger
import java.io.Serializable

data class TaxTypeInsertDto(
    val name: String,
    val description: String? = null,
    val calculationMethod: CalculationMethod,
    val taxRecoveryType: TaxRecoveryType,
    val taxApplicationLevel: TaxApplicationLevel,
    val taxTriggers: List<TaxTrigger>
) : Serializable
