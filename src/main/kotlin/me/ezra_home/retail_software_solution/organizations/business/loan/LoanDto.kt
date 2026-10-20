package me.ezra_home.retail_software_solution.organizations.business.loan

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class LoanDto(
    val id: UUID,
    val referenceNumber: String,
    val lenderContactId: UUID,
    val assetAccountCode: String,
    val amount: BigDecimal,
    val loanDate: LocalDate,
    val note: String?,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)

data class LoanRepaymentDto(
    val id: UUID,
    val referenceNumber: String,
    val loanId: UUID,
    val assetAccountCode: String,
    val amount: BigDecimal,
    val repaymentDate: LocalDate,
    val note: String?,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)

data class LoanVoidDto(
    val id: UUID,
    val referenceNumber: String,
    val loanId: UUID,
    val voidDate: LocalDate,
    val reason: String?,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)

data class LoanRepaymentVoidDto(
    val id: UUID,
    val referenceNumber: String,
    val loanRepaymentId: UUID,
    val voidDate: LocalDate,
    val reason: String?,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)
