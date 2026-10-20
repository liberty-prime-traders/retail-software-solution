package me.ezra_home.retail_software_solution.organizations.business.loan.api

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class LoanResponse(
    val id: UUID,
    val referenceNumber: String,
    val lender: LoanLenderResponse,
    val assetAccount: LoanAccountResponse,
    val amount: BigDecimal,
    val loanDate: LocalDate,
    val note: String?,
    val amountRepaid: BigDecimal,
    val outstanding: BigDecimal,
    val status: LoanStatus,
    val createdOn: OffsetDateTime,
    val createdBy: String,
    val void: LoanVoidResponse?,
    val repayments: List<LoanRepaymentResponse>
)

data class LoanRepaymentResponse(
    val id: UUID,
    val referenceNumber: String,
    val assetAccount: LoanAccountResponse,
    val amount: BigDecimal,
    val repaymentDate: LocalDate,
    val note: String?,
    val createdOn: OffsetDateTime,
    val createdBy: String,
    val void: LoanVoidResponse?
)

data class LoanLenderResponse(
    val contactReference: String,
    val displayName: String
)

data class LoanAccountResponse(
    val code: String,
    val label: String
)

data class LoanVoidResponse(
    val voidDate: LocalDate,
    val reason: String?,
    val voidedBy: String
)
