package me.ezra_home.retail_software_solution.organizations.business.account_selection.api

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountSelectionCandidate
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount

// Flows post to these directly; a custom expense type pointing at one would mix its entries into them.
private val RESERVED_EXPENSE_ACCOUNTS: Set<SystemAccount> = setOf(
    SystemAccount.COST_OF_GOODS_SOLD,
    SystemAccount.WAGES_EXPENSE,
    SystemAccount.INBOUND_FREIGHT,
    SystemAccount.OUTBOUND_FREIGHT,
    SystemAccount.SHRINKAGE_AND_LOSSES,
    SystemAccount.BAD_DEBT_EXPENSE,
    SystemAccount.TAX_EXPENSE
)

enum class AccountSelectionRule(val accountDescription: String) {

    PAYMENT_METHOD("Payment method account") {
        override fun typeAndPlacementRejectionReason(candidate: AccountSelectionCandidate): String? {
            if (!candidate.isAsset()) return "$accountDescription must be of type Asset"
            if (candidate.systemAccount == SystemAccount.CASH) return null
            val parent = candidate.parent ?: return "$accountDescription has no parent: ${candidate.code}"
            if (parent.accountIsSystemMaintained && parent.systemAccount != SystemAccount.DIGITAL_PAYMENTS) {
                return "System-defined payment method account must be a direct child of Digital Payments"
            }
            return null
        }
    },

    TAX_PAYABLE("Payable tax account") {
        override fun typeAndPlacementRejectionReason(candidate: AccountSelectionCandidate): String? = when {
            candidate.accountIsSystemMaintained -> systemParentRejectionReason(candidate, SystemAccount.TAX_PAYABLE)
            !candidate.isLiability() -> "Org-defined payable tax account must be of type Liability"
            else -> null
        }
    },

    TAX_RECOVERABLE("Recoverable tax account") {
        override fun typeAndPlacementRejectionReason(candidate: AccountSelectionCandidate): String? = when {
            candidate.accountIsSystemMaintained -> systemParentRejectionReason(candidate, SystemAccount.TAX_RECOVERABLE)
            !candidate.isAsset() -> "Org-defined recoverable tax account must be of type Asset"
            else -> null
        }
    },

    EXPENSE_TYPE("Expense account") {
        override fun typeAndPlacementRejectionReason(candidate: AccountSelectionCandidate): String? {
            if (!candidate.isExpense()) return "${candidate.label} is not an expense account"
            if (candidate.systemAccount in RESERVED_EXPENSE_ACCOUNTS) {
                return "${candidate.label} is reserved for system flows and cannot back an expense type"
            }
            return null
        }
    };

    protected abstract fun typeAndPlacementRejectionReason(candidate: AccountSelectionCandidate): String?

    // Ledger postings are rejected on a parent account, so every rule offers leaves only.
    fun rejectionReason(candidate: AccountSelectionCandidate): String? {
        if (!candidate.isLeaf) return "${candidate.label} has child accounts; $accountDescription must be a leaf account"
        return typeAndPlacementRejectionReason(candidate)
    }

    fun isSelectable(candidate: AccountSelectionCandidate): Boolean = rejectionReason(candidate) == null

    protected fun systemParentRejectionReason(candidate: AccountSelectionCandidate, requiredParent: SystemAccount): String? {
        val parent = candidate.parent ?: return "$accountDescription has no parent: ${candidate.code}"
        return if (parent.systemAccount == requiredParent) null
        else "$accountDescription must be under the ${requiredParent.name} parent account"
    }
}
