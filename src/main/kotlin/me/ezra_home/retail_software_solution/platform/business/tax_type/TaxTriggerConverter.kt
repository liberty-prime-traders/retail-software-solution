package me.ezra_home.retail_software_solution.platform.business.tax_type

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumListConverter

@Converter(autoApply = true)
class TaxTriggerConverter : EnumListConverter<TaxTrigger>(TaxTrigger::class.java)
