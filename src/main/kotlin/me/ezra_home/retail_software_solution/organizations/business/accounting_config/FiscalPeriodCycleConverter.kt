package me.ezra_home.retail_software_solution.organizations.business.accounting_config

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.FiscalPeriodCycle
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class FiscalPeriodCycleConverter : EnumConverter<FiscalPeriodCycle>(FiscalPeriodCycle::class.java)
