package me.ezra_home.retail_software_solution.organizations.business.fund_movement

import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class FundMovementDto(
    val id: UUID,
    val referenceNumber: String,
    val type: FundMovementType,
    val debitAccountCode: String,
    val creditAccountCode: String,
    val amount: BigDecimal,
    val movementDate: LocalDate,
    val note: String?,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)

data class FundMovementVoidDto(
    val id: UUID,
    val referenceNumber: String,
    val fundMovementId: UUID,
    val voidDate: LocalDate,
    val reason: String?,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)

data class FundMovementWithVoid(
    val fundMovementDto: FundMovementDto,
    val fundMovementVoidDto: FundMovementVoidDto?
)
