package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class MovementTypeConverter : EnumConverter<MovementType>(MovementType::class.java)
