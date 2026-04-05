package me.ezra_home.retail_software_solution.organizations.business.inventory

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
internal class MovementReasonConverter : EnumConverter<MovementReason>(MovementReason::class.java)
