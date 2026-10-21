package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect
import java.math.BigDecimal

data class SubledgerBalanceDto(
    val payable: BigDecimal,
    val receivable: BigDecimal
) {
    val netBalance: BigDecimal = receivable.subtract(payable).abs()

    // Read from the org's POV: the contact owing us is money the org stands to receive.
    val balanceEffect: BalanceEffect = BalanceEffect.ofSignedAmount(receivable.subtract(payable))

    companion object {
        val ZERO = SubledgerBalanceDto(
            payable = Decimals.Scale4.round(BigDecimal.ZERO),
            receivable = Decimals.Scale4.round(BigDecimal.ZERO)
        )
    }
}
