package me.ezra_home.retail_software_solution.platform.business.authorization_pass

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.`public`.PassStatus
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class PassStatusConverter : EnumConverter<PassStatus>(PassStatus::class.java)
