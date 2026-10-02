package me.ezra_home.retail_software_solution.locations.business.tax_entry

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxDirection
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class TaxDirectionConverter : EnumConverter<TaxDirection>(TaxDirection::class.java)
