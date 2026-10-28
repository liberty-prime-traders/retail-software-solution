package me.ezra_home.retail_software_solution.util.ui_models

import java.math.BigDecimal

enum class BalanceEffect {
    ADDS,
    SUBTRACTS,
    ZERO,
    IRREGULAR;

    private fun opposite() = when (this) {
        ADDS -> SUBTRACTS
        SUBTRACTS -> ADDS
        else -> this
    }

    companion object {
        fun ofBalance(balance: BigDecimal, effectWhenNormal: BalanceEffect): BalanceEffect = when {
            balance.signum() == 0 -> ZERO
            balance.signum() < 0 -> IRREGULAR
            else -> effectWhenNormal
        }

        fun ofChange(change: BigDecimal, effectWhenIncreasing: BalanceEffect): BalanceEffect = when {
            change.signum() == 0 -> ZERO
            change.signum() > 0 -> effectWhenIncreasing
            else -> effectWhenIncreasing.opposite()
        }

        fun ofSignedAmount(amount: BigDecimal): BalanceEffect = ofChange(amount, ADDS)
    }
}
