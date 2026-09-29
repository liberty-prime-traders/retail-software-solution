package me.ezra_home.retail_software_solution.organizations.business.account

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class AccountTypeConverter : EnumConverter<AccountType>(AccountType::class.java)
