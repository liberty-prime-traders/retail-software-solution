package me.ezra_home.retail_software_solution.util.business

import com.fasterxml.jackson.annotation.JsonCreator
import java.math.BigDecimal
import java.math.RoundingMode

// Rejects negatives, excess decimals and a magnitude beyond MAXIMUM_TRANSACTION_AMOUNT instead of rounding;
// not for a derived price, which can carry more decimals.
class Money private constructor(typedValue: BigDecimal) : TypedDecimal<Money>(typedValue) {

    operator fun plus(other: Money): Money = of(toBigDecimal().add(other.toBigDecimal()))

    companion object {
        val ZERO: Money = of(BigDecimal.ZERO)

        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun of(typedValue: BigDecimal): Money {
            Decimals.requireMaxDecimalPlaces(typedValue, Decimals.MoneyScale.scale, "Amount")
            Decimals.requireNonNegative(typedValue, "Amount")
            Decimals.requireWithinMaximum(typedValue, Decimals.MAXIMUM_TRANSACTION_AMOUNT, "Amount")
            return Money(typedValue.setScale(Decimals.MoneyScale.scale, RoundingMode.UNNECESSARY))
        }
    }
}
