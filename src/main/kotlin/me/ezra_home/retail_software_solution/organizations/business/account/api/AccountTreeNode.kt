package me.ezra_home.retail_software_solution.organizations.business.account.api

import java.math.BigDecimal

data class AccountTreeNode(
    val code: String,
    val name: String,
    val parentAccountCode: String?,
    val category: AccountCategory,
    val isContra: Boolean,
    val normalBalance: EntryType,
    val isLeaf: Boolean
) {
    fun amountInNormalDirection(entryType: EntryType, amount: BigDecimal): BigDecimal =
        if (entryType == normalBalance) amount else amount.negate()
}
