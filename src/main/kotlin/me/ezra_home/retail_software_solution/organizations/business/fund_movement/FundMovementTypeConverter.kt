package me.ezra_home.retail_software_solution.organizations.business.fund_movement

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementType
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter
class FundMovementTypeConverter : EnumConverter<FundMovementType>(FundMovementType::class.java)
