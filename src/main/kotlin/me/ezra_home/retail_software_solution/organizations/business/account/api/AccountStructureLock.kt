package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import org.springframework.stereotype.Component

// Guards an account's position in the tree — specifically, whether it's a leaf (has no
// children) — against three features that each run a check-then-write on that same fact:
//
//   - AccountService.createChild reads the parent's current children, then inserts a new one.
//   - CoaDefaultsInserter.seedDefaults reads the current chart of accounts, then bulk-inserts any
//     missing SystemAccount children (e.g. seeding CASH under CURRENT_ASSETS).
//   - OpeningBalanceService.upsert reads whether the account has children, then, if it doesn't,
//     writes an opening balance for it.
//
// Without a shared lock, any two of these can interleave: opening_balance reads "no children
// yet" on some account, then before it writes, a child gets created under that same account by
// either of the other two, and the opening balance still gets written — leaving a non-leaf
// account with an opening balance, which should never be possible.
//
// Acquire this before doing the read, and hold it for the whole transaction (it's a
// pg_advisory_xact_lock under the hood, released on commit/rollback). A row lock on the account
// itself wouldn't do the same job here: none of the three updates the account row being read, so
// there's nothing for a row lock to catch.
@Component
class AccountStructureLock(
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock
) {
    fun acquire(accountCode: String) {
        orgEntityAdvisoryLock.acquire(LockNamespaces.ACCOUNT, accountCode)
    }

    fun acquire(accountCodes: Collection<String>) {
        if (accountCodes.isEmpty()) return
        orgEntityAdvisoryLock.acquire(LockNamespaces.ACCOUNT, accountCodes)
    }
}
