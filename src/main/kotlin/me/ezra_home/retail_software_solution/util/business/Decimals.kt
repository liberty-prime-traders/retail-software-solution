package me.ezra_home.retail_software_solution.util.business

import java.math.BigDecimal
import java.math.RoundingMode

object Decimals {

    object Scale4 {
        private const val SCALE = 4

        fun round(value: BigDecimal): BigDecimal = value.setScale(SCALE, RoundingMode.HALF_UP)

        fun multiply(value: BigDecimal, factor: BigDecimal): BigDecimal =
            value.multiply(factor).setScale(SCALE, RoundingMode.HALF_UP)

        fun divide(value: BigDecimal, divisor: BigDecimal): BigDecimal =
            value.divide(divisor, SCALE, RoundingMode.HALF_UP)

        fun compare(first: BigDecimal, second: BigDecimal): Int = compareAtScale(first, second, SCALE)
    }

    // Stock quantities are stored at this scale, so a conversion rounded here is not rounded again on save.
    object Scale10 {
        private const val SCALE = 10

        fun divide(value: BigDecimal, divisor: BigDecimal): BigDecimal =
            value.divide(divisor, SCALE, RoundingMode.HALF_UP)
    }

    fun compareAtScale(first: BigDecimal, second: BigDecimal, scale: Int): Int =
        first.setScale(scale, RoundingMode.HALF_UP).compareTo(second.setScale(scale, RoundingMode.HALF_UP))

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

    fun gcd(a: Long, b: Long): Long {
        val absA = if (a < 0) -a else a
        val absB = if (b < 0) -b else b
        return if (absB == 0L) absA else gcd(absB, absA % absB)
    }
}
