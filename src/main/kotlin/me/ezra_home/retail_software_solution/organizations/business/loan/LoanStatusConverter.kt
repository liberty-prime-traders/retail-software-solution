package me.ezra_home.retail_software_solution.organizations.business.loan

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanStatus
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class LoanStatusConverter : EnumConverter<LoanStatus>(LoanStatus::class.java)
