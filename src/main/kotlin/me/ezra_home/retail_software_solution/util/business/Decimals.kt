package me.ezra_home.retail_software_solution.util.business

import java.math.BigDecimal
import java.math.RoundingMode

object Decimals {
    fun multiplyScale4(value: BigDecimal, factor: BigDecimal): BigDecimal =
        value.multiply(factor).setScale(4, RoundingMode.HALF_UP)

    fun divideScale4(value: BigDecimal, divisor: BigDecimal): BigDecimal {
        return value.divide(divisor, 4, RoundingMode.HALF_UP)
    }

    fun stripZeroesAndRound(quantity: BigDecimal): String {
        return when {
            quantity.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0 ->
                quantity.toBigInteger().toString()
            else ->
                quantity.setScale(4, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString()
        }
    }
}
