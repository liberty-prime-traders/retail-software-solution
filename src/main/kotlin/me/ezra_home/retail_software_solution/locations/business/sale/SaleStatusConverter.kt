package me.ezra_home.retail_software_solution.locations.business.sale

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class SaleStatusConverter : EnumConverter<SaleStatus>(SaleStatus::class.java)
