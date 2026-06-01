package me.ezra_home.retail_software_solution.util.business

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency

object Currencies {

    fun format(decimal: BigDecimal): String {
        val formatter = NumberFormat.getCurrencyInstance()
        formatter.currency = Currency.getInstance("KES")
        return formatter.format(decimal)
    }
}
