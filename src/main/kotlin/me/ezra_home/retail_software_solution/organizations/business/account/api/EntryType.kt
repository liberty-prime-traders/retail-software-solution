package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.util.enums.HasCode
import java.math.BigDecimal

enum class EntryType(override val code: String) : HasCode {
    DEBIT("DB"),
    CREDIT("CR");

    fun opposite(): EntryType = when (this) {
        DEBIT -> CREDIT
        CREDIT -> DEBIT
    }

    // Positive when this entry is on the account's normal side, so it increases the balance; negative on the other side.
    fun amountTowards(normalBalance: EntryType, amount: BigDecimal): BigDecimal =
        if (this == normalBalance) amount else amount.negate()
}
