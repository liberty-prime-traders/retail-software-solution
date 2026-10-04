package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountStructureLock
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import org.springframework.stereotype.Component

// Contacts are advisory-locked because subledger_entry is append-only: a row lock on the latest
// row goes stale once a second posting appends, and a contact's first posting has no row to lock.
// Accounts are always acquired before contacts so every writer shares one lock order.
@Component
class AccountsAndLedgerLock(
    private val accountStructureLock: AccountStructureLock,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock
) {
    fun acquire(ledgerPostingRequest: LedgerPostingRequest) {
        accountStructureLock.acquire(ledgerPostingRequest.entries.map { it.accountCode })
        val contactReferenceNumbers = ledgerPostingRequest.subledgerEntries.map { it.contactReferenceNumber }
        if (contactReferenceNumbers.isNotEmpty()) {
            orgEntityAdvisoryLock.acquire(LockNamespaces.SUBLEDGER_CONTACT, contactReferenceNumbers)
        }
    }
}
