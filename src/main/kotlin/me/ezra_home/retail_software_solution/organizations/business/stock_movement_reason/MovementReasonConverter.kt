package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class MovementReasonConverter : EnumConverter<MovementReason>(MovementReason::class.java)
