package me.ezra_home.retail_software_solution.platform.business.tax_type.api

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class TaxCalculationMethodConverter : EnumConverter<TaxCalculationMethod>(TaxCalculationMethod::class.java)
