package me.ezra_home.retail_software_solution.util.quantity

import com.fasterxml.jackson.annotation.JsonCreator
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.TypedDecimal
import java.math.BigDecimal

class UserProvidedQuantity private constructor(typedValue: BigDecimal) : TypedDecimal<UserProvidedQuantity>(typedValue) {

    fun isZero(): Boolean = toBigDecimal().signum() == 0

    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun of(typedValue: BigDecimal): UserProvidedQuantity {
            Decimals.requireMaxDecimalPlaces(typedValue, QuantityLimits.MAX_USER_QUANTITY_DECIMAL_PLACES, "Quantity")
            Decimals.requireNonNegative(typedValue, "Quantity")
            Decimals.requireWithinMaximum(typedValue, QuantityLimits.MAX_USER_QUANTITY, "Quantity")
            return UserProvidedQuantity(typedValue)
        }
    }
}
