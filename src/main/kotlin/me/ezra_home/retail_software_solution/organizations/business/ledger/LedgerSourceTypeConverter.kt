package me.ezra_home.retail_software_solution.organizations.business.ledger

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class LedgerSourceTypeConverter : EnumConverter<LedgerSourceType>(LedgerSourceType::class.java)
