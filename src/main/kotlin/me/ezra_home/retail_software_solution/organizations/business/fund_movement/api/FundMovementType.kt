package me.ezra_home.retail_software_solution.organizations.business.fund_movement.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class FundMovementType(override val code: String) : HasCode {
    TRANSFER_BETWEEN_ACCOUNTS("TRF"),
    CONTRIBUTE_CAPITAL_TO_BUSINESS("CPT"),
    WITHDRAW_FOR_PERSONAL_USE("DRW")
}
