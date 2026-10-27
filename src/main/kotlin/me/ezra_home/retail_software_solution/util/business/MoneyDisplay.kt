package me.ezra_home.retail_software_solution.util.business

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.math.BigDecimal

// Output only: rounds to the money scale and never throws, so it holds signed, derived or oversized amounts.
// The Kotlin Jackson module treats a private constructor as a creator unless it is disabled, so binding is switched off explicitly.
// No arithmetic, so it cannot round a figure that is still being summed.
class MoneyDisplay @JsonCreator(mode = JsonCreator.Mode.DISABLED) private constructor(private val displayValue: BigDecimal) : Comparable<MoneyDisplay>, Serializable {

    @JsonValue
    fun toBigDecimal(): BigDecimal = displayValue

    override fun compareTo(other: MoneyDisplay): Int = displayValue.compareTo(other.displayValue)

    override fun equals(other: Any?): Boolean = other is MoneyDisplay && displayValue.compareTo(other.displayValue) == 0

    override fun hashCode(): Int = displayValue.stripTrailingZeros().hashCode()

    override fun toString(): String = displayValue.toPlainString()

    companion object {
        fun of(amount: BigDecimal): MoneyDisplay = MoneyDisplay(Decimals.MoneyScale.round(amount))

        fun of(money: Money): MoneyDisplay = MoneyDisplay(money.toBigDecimal())

        fun of(positiveMoney: PositiveMoney): MoneyDisplay = MoneyDisplay(positiveMoney.toBigDecimal())
    }
}
