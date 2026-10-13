package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.locations.business.stock.api.MovementType
import me.ezra_home.retail_software_solution.util.enums.EnumSetConverter

@Converter(autoApply = true)
class MovementTypeSetConverter : EnumSetConverter<MovementType>(MovementType::class.java)
