package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.ui_models.BalanceSignal
import java.math.BigDecimal

data class SubledgerBalanceDto(
    val payable: BigDecimal,
    val receivable: BigDecimal
) {
    val netBalance: BigDecimal = receivable.subtract(payable).abs()

    // Read from the org's POV: the contact owing us is money the org stands to receive.
    val balanceSignal: BalanceSignal = when {
        receivable.compareTo(payable) == 0 -> BalanceSignal.ZERO_BALANCE
        receivable > payable -> BalanceSignal.MONEY_IN
        else -> BalanceSignal.MONEY_OUT
    }

    companion object {
        val ZERO = SubledgerBalanceDto(
            payable = Decimals.Scale4.round(BigDecimal.ZERO),
            receivable = Decimals.Scale4.round(BigDecimal.ZERO)
        )
    }
}
