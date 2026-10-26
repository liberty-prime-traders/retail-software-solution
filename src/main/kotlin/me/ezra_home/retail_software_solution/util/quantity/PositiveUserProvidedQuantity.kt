package me.ezra_home.retail_software_solution.util.quantity

import com.fasterxml.jackson.annotation.JsonCreator
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.TypedDecimal
import java.math.BigDecimal

// A UserProvidedQuantity that must also be above zero
class PositiveUserProvidedQuantity private constructor(typedValue: BigDecimal) : TypedDecimal<PositiveUserProvidedQuantity>(typedValue) {

    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun of(typedValue: BigDecimal): PositiveUserProvidedQuantity {
            val quantity = UserProvidedQuantity.of(typedValue).toBigDecimal()
            Decimals.requirePositive(quantity, "Quantity")
            return PositiveUserProvidedQuantity(quantity)
        }
    }
}
