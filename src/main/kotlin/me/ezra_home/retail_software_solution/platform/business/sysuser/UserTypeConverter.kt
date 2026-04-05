package me.ezra_home.retail_software_solution.platform.business.sysuser

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.UserType
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class UserTypeConverter : EnumConverter<UserType>(UserType::class.java)
