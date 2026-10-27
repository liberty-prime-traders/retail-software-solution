package me.ezra_home.retail_software_solution.util.business

import com.fasterxml.jackson.annotation.JsonCreator
import java.math.BigDecimal
import java.math.RoundingMode

// Rejects negatives, excess decimals and anything above 100 instead of rounding. Keeps 2 decimals whatever the money scale.
class Percentage private constructor(typedValue: BigDecimal) : TypedDecimal<Percentage>(typedValue) {

    companion object {
        private const val MAXIMUM_DECIMAL_PLACES = 2
        private val MAXIMUM_PERCENTAGE = BigDecimal(100)

        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun of(typedValue: BigDecimal): Percentage {
            Decimals.requireMaxDecimalPlaces(typedValue, MAXIMUM_DECIMAL_PLACES, "Percentage")
            Decimals.requireNonNegative(typedValue, "Percentage")
            Decimals.requireWithinMaximum(typedValue, MAXIMUM_PERCENTAGE, "Percentage")
            return Percentage(typedValue.setScale(MAXIMUM_DECIMAL_PLACES, RoundingMode.UNNECESSARY))
        }
    }
}
