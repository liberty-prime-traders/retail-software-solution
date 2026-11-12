package me.ezra_home.retail_software_solution.organizations.business.opening_balance

import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import java.math.BigDecimal
import java.time.LocalDate

object OpeningBalancePostingRequests {

    fun revised(
        ledgerSourceReferenceNumber: String,
        accountCode: String,
        normalBalanceEntryType: EntryType,
        signedDelta: BigDecimal,
        postingDate: LocalDate
    ): LedgerPostingRequest {
        val positiveAmount = PositiveMoney.of(signedDelta.abs())
        val accountEntryType = entryTypeFor(normalBalanceEntryType, signedDelta)
        return LedgerPostingRequest(
            sourceReferenceNumber = ledgerSourceReferenceNumber,
            sourceType = LedgerSourceType.OPENING_BALANCE,
            postingDate = postingDate,
            entries = listOf(
                LedgerEntryRequest(accountCode, accountEntryType, positiveAmount),
                LedgerEntryRequest(SystemAccount.OPENING_BALANCE_EQUITY.code, accountEntryType.opposite(), positiveAmount)
            ),
            subledgerEntries = emptyList()
        )
    }

    private fun entryTypeFor(normalBalanceEntryType: EntryType, signedDelta: BigDecimal): EntryType {
        val isCorrectionReducingBalance = signedDelta.signum() < 0
        return if (isCorrectionReducingBalance) normalBalanceEntryType.opposite() else normalBalanceEntryType
    }
}
