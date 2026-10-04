package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import org.springframework.stereotype.Component

// Guards an account's position in the tree — specifically, whether it's a leaf (has no
// children) — against features that each run a check-then-write on that same fact: createChild,
// seedDefaults, OpeningBalanceService.upsert, and ledger postings (via AccountsAndLedgerLock).
//
// Without a shared lock they can interleave: a posting or opening balance reads "no children yet",
// a child is created under that account before it writes, and the non-leaf account still receives
// the entry — which should never be possible.
//
// Acquire this before doing the read, and hold it for the whole transaction (it's a
// pg_advisory_xact_lock under the hood, released on commit/rollback). A row lock on the account
// itself wouldn't do the same job here: none of these updates the account row being read, so
// there's nothing for a row lock to catch.
@Component
class AccountStructureLock(
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock
) {
    fun acquire(accountCode: String) {
        acquire(listOf(accountCode))
    }

    fun acquire(accountCodes: Collection<String>) {
        if (accountCodes.isEmpty()) return
        orgEntityAdvisoryLock.acquire(LockNamespaces.ACCOUNT, accountCodes)
    }
}
