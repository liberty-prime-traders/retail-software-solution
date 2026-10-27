package me.ezra_home.retail_software_solution.util.business

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object Decimals {
    // Why 12 integer digits against a numeric(19,4) column: util/business/README.md rule 7.
    val MAXIMUM_TRANSACTION_AMOUNT: BigDecimal = BigDecimal("999999999999.99")

    sealed class DecimalScale {
        abstract val scale: Int

        fun round(value: BigDecimal): BigDecimal = value.setScale(scale, RoundingMode.HALF_UP)

        fun roundDown(value: BigDecimal): BigDecimal = value.setScale(scale, RoundingMode.DOWN)

        fun roundUp(value: BigDecimal): BigDecimal = value.setScale(scale, RoundingMode.UP)

        fun multiply(value: BigDecimal, factor: BigDecimal): BigDecimal =
            value.multiply(factor).setScale(scale, RoundingMode.HALF_UP)

        fun divide(value: BigDecimal, divisor: BigDecimal): BigDecimal =
            value.divide(divisor, scale, RoundingMode.HALF_UP)

        fun compare(first: BigDecimal, second: BigDecimal): Int = compareAtScale(first, second, this)
    }

    // Money columns are numeric(19,4) but round to 2: util/business/README.md rule 7.
    const val MONEY_SCALE = 2

    sealed class FixedScale(final override val scale: Int) : DecimalScale()
    object Scale4 : FixedScale(4)
    object Scale10 : FixedScale(10)
    object MoneyScale : FixedScale(MONEY_SCALE)
    object PercentDisplayScale : FixedScale(1)

    fun compareAtScale(first: BigDecimal, second: BigDecimal, decimalScale: DecimalScale): Int =
        first.setScale(decimalScale.scale, RoundingMode.HALF_UP).compareTo(second.setScale(decimalScale.scale, RoundingMode.HALF_UP))

    fun stripZeroesAndRound(quantity: BigDecimal): String = stripZeroes(quantity, RoundingMode.HALF_UP)

    fun stripZeroesAndRoundNeverToZero(value: BigDecimal): String =
        if (value.signum() != 0 && Scale4.round(value).signum() == 0) {
            value.round(MathContext(1, RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString()
        } else {
            stripZeroes(value, RoundingMode.HALF_UP)
        }

    private fun stripZeroes(quantity: BigDecimal, roundingMode: RoundingMode): String =
        quantity.setScale(Scale4.scale, roundingMode).stripTrailingZeros().toPlainString()

    fun requireMaxDecimalPlaces(value: BigDecimal, maxDecimalPlaces: Int, label: String) {
        if (value.stripTrailingZeros().scale() > maxDecimalPlaces) {
            throw RtsGenericException("$label can have at most $maxDecimalPlaces decimal places")
        }
    }

    fun requireWithinMaximum(value: BigDecimal, maximum: BigDecimal, label: String) {
        if (value.abs() > maximum) {
            throw RtsGenericException("$label must not exceed ${maximum.toPlainString()}")
        }
    }

    fun requireNonNegative(value: BigDecimal, label: String) {
        if (value.signum() < 0) {
            throw RtsGenericException("$label must not be negative")
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
