package me.ezra_home.retail_software_solution.organizations.business.fund_movement

import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import java.math.BigDecimal
import java.time.LocalDate

object FundMovementPostingRequests {

    fun recorded(
        referenceNumber: String,
        debitAccountCode: String,
        creditAccountCode: String,
        amount: BigDecimal,
        movementDate: LocalDate
    ): LedgerPostingRequest = LedgerPostingRequest(
        sourceReferenceNumber = referenceNumber,
        sourceType = LedgerSourceType.FUND_MOVEMENT,
        postingDate = movementDate,
        entries = listOf(
            LedgerEntryRequest.debit(debitAccountCode, amount),
            LedgerEntryRequest.credit(creditAccountCode, amount)
        ),
        subledgerEntries = emptyList()
    )

    // The movement's own debit and credit accounts, legs swapped; the void shares the movement's reference.
    fun voided(
        referenceNumber: String,
        debitAccountCode: String,
        creditAccountCode: String,
        amount: BigDecimal,
        voidDate: LocalDate
    ): LedgerPostingRequest = LedgerPostingRequest(
        sourceReferenceNumber = referenceNumber,
        sourceType = LedgerSourceType.FUND_MOVEMENT_VOID,
        postingDate = voidDate,
        entries = listOf(
            LedgerEntryRequest.credit(debitAccountCode, amount),
            LedgerEntryRequest.debit(creditAccountCode, amount)
        ),
        subledgerEntries = emptyList(),
        reversal = true
    )
}
