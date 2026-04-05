package me.ezra_home.retail_software_solution.platform.business.tax_type

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.platform.business.tax_type.`public`.TaxRecoveryType
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class TaxRecoveryTypeConverter : EnumConverter<TaxRecoveryType>(TaxRecoveryType::class.java)
