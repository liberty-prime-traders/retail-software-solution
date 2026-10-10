package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.ui_models.BalanceSignal
import java.math.BigDecimal

data class SubledgerBalanceDto(
    val payable: BigDecimal,
    val receivable: BigDecimal
) {
    val netBalance: BigDecimal = receivable.subtract(payable).abs()

    val balanceSignal: BalanceSignal = when {
        receivable.compareTo(payable) == 0 -> BalanceSignal.ZERO_BALANCE
        receivable > payable -> BalanceSignal.MONEY_OUT
        else -> BalanceSignal.MONEY_IN
    }

    companion object {
        val ZERO = SubledgerBalanceDto(
            payable = Decimals.roundToScale4(BigDecimal.ZERO),
            receivable = Decimals.roundToScale4(BigDecimal.ZERO)
        )
    }
}
