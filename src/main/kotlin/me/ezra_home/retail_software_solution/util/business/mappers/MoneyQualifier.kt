package me.ezra_home.retail_software_solution.util.business.mappers

import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.MoneyDisplay
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
object MoneyQualifier {
    fun toBigDecimal(money: Money?): BigDecimal? = money?.toBigDecimal()

    fun toBigDecimal(positiveMoney: PositiveMoney?): BigDecimal? = positiveMoney?.toBigDecimal()

    fun toMoneyDisplay(amount: BigDecimal?): MoneyDisplay? = amount?.let { MoneyDisplay.of(it) }
}
