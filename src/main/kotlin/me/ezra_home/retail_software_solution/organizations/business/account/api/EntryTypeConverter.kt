package me.ezra_home.retail_software_solution.organizations.business.account.api

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class EntryTypeConverter : EnumConverter<EntryType>(EntryType::class.java)
