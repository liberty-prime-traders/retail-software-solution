package me.ezra_home.retail_software_solution.util.business

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency

object DisplayFormatters {

    fun formatCurrency(decimal: BigDecimal): String {
        val formatter = NumberFormat.getCurrencyInstance()
        formatter.currency = Currency.getInstance("KES")
        return formatter.format(decimal)
    }

    fun pluralize(text: String, quantity: BigDecimal): String {
        return if (quantity.compareTo(BigDecimal.ONE) == 0) {
            text
        } else {
            "${text}s"
        }
    }
}
