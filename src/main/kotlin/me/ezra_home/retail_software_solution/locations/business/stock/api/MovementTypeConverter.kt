package me.ezra_home.retail_software_solution.locations.business.stock.api

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class MovementTypeConverter : EnumConverter<MovementType>(MovementType::class.java)
