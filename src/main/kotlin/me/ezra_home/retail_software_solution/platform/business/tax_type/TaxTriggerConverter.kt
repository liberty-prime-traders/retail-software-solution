package me.ezra_home.retail_software_solution.platform.business.tax_type

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxTrigger
import me.ezra_home.retail_software_solution.util.enums.EnumSetConverter

@Converter(autoApply = true)
class TaxTriggerConverter : EnumSetConverter<TaxTrigger>(TaxTrigger::class.java)
