package me.ezra_home.retail_software_solution.platform.business.tax_type.api

import java.io.Serializable

data class TaxTypeInsertDto(
    val name: String,
    val description: String? = null,
    val calculationMethod: CalculationMethod,
    val taxRecoveryType: TaxRecoveryType,
    val taxApplicationLevel: TaxApplicationLevel,
    val taxTriggers: List<TaxTrigger>
) : Serializable
