package me.ezra_home.retail_software_solution.organizations.business.fund_movement.api

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class FundMovementResponse(
    val id: UUID,
    val referenceNumber: String,
    val type: FundMovementType,
    val debitAccount: FundMovementAccountResponse,
    val creditAccount: FundMovementAccountResponse,
    val amount: BigDecimal,
    val movementDate: LocalDate,
    val note: String?,
    val createdOn: OffsetDateTime,
    val createdBy: String,
    val void: FundMovementVoidResponse?
)

data class FundMovementAccountResponse(
    val code: String,
    val label: String
)

data class FundMovementVoidResponse(
    val voidDate: LocalDate,
    val reason: String?,
    val voidedBy: String
)
