package me.ezra_home.retail_software_solution.organizations.business.adjustment_reason

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class AdjustmentDirectionConverter :
    EnumConverter<AdjustmentDirection>(AdjustmentDirection::class.java)
