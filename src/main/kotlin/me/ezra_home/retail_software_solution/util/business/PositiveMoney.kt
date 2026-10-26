package me.ezra_home.retail_software_solution.util.business

import com.fasterxml.jackson.annotation.JsonCreator
import java.math.BigDecimal

class PositiveMoney private constructor(typedValue: BigDecimal) : TypedDecimal<PositiveMoney>(typedValue) {

    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun of(typedValue: BigDecimal): PositiveMoney {
            val money = Money.of(typedValue).toBigDecimal()
            Decimals.requirePositive(money, "Amount")
            return PositiveMoney(money)
        }
    }
}
