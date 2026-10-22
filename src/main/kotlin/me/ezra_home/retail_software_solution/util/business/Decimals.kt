package me.ezra_home.retail_software_solution.util.business

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object Decimals {
    // numeric(19, 4) holds 15 integer digits; the 12 integer-digit ceiling leaves headroom for balances to sum, whatever the money scale.
    val MAXIMUM_TRANSACTION_AMOUNT: BigDecimal = BigDecimal("999999999999.99")

    abstract class FixedScale(val scale: Int) {
        fun round(value: BigDecimal): BigDecimal = value.setScale(scale, RoundingMode.HALF_UP)

        fun roundDown(value: BigDecimal): BigDecimal = value.setScale(scale, RoundingMode.DOWN)

        fun roundUp(value: BigDecimal): BigDecimal = value.setScale(scale, RoundingMode.UP)

        fun multiply(value: BigDecimal, factor: BigDecimal): BigDecimal =
            value.multiply(factor).setScale(scale, RoundingMode.HALF_UP)

        fun divide(value: BigDecimal, divisor: BigDecimal): BigDecimal =
            value.divide(divisor, scale, RoundingMode.HALF_UP)

        fun compare(first: BigDecimal, second: BigDecimal): Int = compareAtScale(first, second, scale)

        fun requirePositiveAmount(amount: BigDecimal, label: String = "Amount"): BigDecimal {
            val roundedAmount = round(amount)
            requirePositive(roundedAmount, label)
            if (roundedAmount > MAXIMUM_TRANSACTION_AMOUNT) {
                throw RtsGenericException("$label must not exceed ${MAXIMUM_TRANSACTION_AMOUNT.toPlainString()}")
            }
            return roundedAmount
        }
    }

    object Scale4 : FixedScale(4)

    object Scale2 : FixedScale(2)

    object Scale10 {
        private const val SCALE = 10

        fun divide(value: BigDecimal, divisor: BigDecimal, roundingMode: RoundingMode = RoundingMode.HALF_UP): BigDecimal =
            value.divide(divisor, SCALE, roundingMode)
    }

    fun compareAtScale(first: BigDecimal, second: BigDecimal, scale: Int): Int =
        first.setScale(scale, RoundingMode.HALF_UP).compareTo(second.setScale(scale, RoundingMode.HALF_UP))

    fun stripZeroesAndRound(quantity: BigDecimal): String = stripZeroes(quantity, RoundingMode.HALF_UP)

    fun stripZeroesAndRoundDown(quantity: BigDecimal): String = stripZeroes(quantity, RoundingMode.DOWN)

    fun stripZeroesAndRoundNeverToZero(value: BigDecimal): String =
        if (value.signum() != 0 && Scale4.round(value).signum() == 0) {
            value.round(MathContext(1, RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString()
        } else {
            stripZeroes(value, RoundingMode.HALF_UP)
        }

    private fun stripZeroes(quantity: BigDecimal, roundingMode: RoundingMode): String =
        quantity.setScale(4, roundingMode).stripTrailingZeros().toPlainString()

    fun requireMaxDecimalPlaces(value: BigDecimal, maxDecimalPlaces: Int, label: String) {
        if (value.stripTrailingZeros().scale() > maxDecimalPlaces) {
            throw RtsGenericException("$label can have at most $maxDecimalPlaces decimal places")
        }
    }

    fun requirePositive(value: BigDecimal, label: String) {
        if (value.signum() <= 0) {
            throw RtsGenericException("$label must be greater than zero")
        }
    }

    fun gcd(a: Long, b: Long): Long {
        val absA = if (a < 0) -a else a
        val absB = if (b < 0) -b else b
        return if (absB == 0L) absA else gcd(absB, absA % absB)
    }
}
