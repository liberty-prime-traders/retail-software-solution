package me.ezra_home.retail_software_solution.organizations.business.account

import java.math.BigDecimal

interface AccountBalance {
    val code: String
    val currentBalance: BigDecimal
}
