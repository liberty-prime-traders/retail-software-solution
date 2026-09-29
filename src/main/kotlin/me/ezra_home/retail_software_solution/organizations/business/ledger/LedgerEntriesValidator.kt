package me.ezra_home.retail_software_solution.organizations.business.ledger

import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal

object LedgerEntriesValidator {

    fun validate(entries: List<LedgerEntryRequest>) {
        val totalDebits = entries
            .filter { it.entryType == EntryType.DEBIT }
            .fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }
        val totalCredits = entries
            .filter { it.entryType == EntryType.CREDIT }
            .fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw RtsGenericException("Ledger entries are not balanced — debits: $totalDebits, credits: $totalCredits")
        }
    }
}
