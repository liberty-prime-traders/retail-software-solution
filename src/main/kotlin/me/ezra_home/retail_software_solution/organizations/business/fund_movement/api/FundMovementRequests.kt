package me.ezra_home.retail_software_solution.organizations.business.fund_movement.api

import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import java.time.LocalDate
import java.util.UUID

data class FundMovementTransferRequest(
    val fromAccountCode: String,
    val toAccountCode: String,
    val amount: PositiveMoney,
    val movementDate: LocalDate,
    val note: String? = null
)

data class FundMovementCapitalRequest(
    val accountCode: String,
    val amount: PositiveMoney,
    val movementDate: LocalDate,
    val note: String? = null
)

data class FundMovementDrawRequest(
    val accountCode: String,
    val amount: PositiveMoney,
    val movementDate: LocalDate,
    val note: String? = null
)

data class FundMovementVoidRequest(
    val id: UUID,
    val reason: String? = null
)
