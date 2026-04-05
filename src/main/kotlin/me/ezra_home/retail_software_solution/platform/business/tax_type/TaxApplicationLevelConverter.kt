package me.ezra_home.retail_software_solution.platform.business.tax_type

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
internal class TaxApplicationLevelConverter : EnumConverter<TaxApplicationLevel>(TaxApplicationLevel::class.java)
