package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class PriceAdjustmentMethodConverter : EnumConverter<PriceAdjustmentMethod>(PriceAdjustmentMethod::class.java)
