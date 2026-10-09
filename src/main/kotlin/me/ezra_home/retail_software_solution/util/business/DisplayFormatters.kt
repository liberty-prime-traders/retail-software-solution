package me.ezra_home.retail_software_solution.util.business

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object DisplayFormatters {

    // DecimalFormat keeps mutable parse/format buffers, so a shared instance corrupts output under concurrent calls.
    private fun buildCurrencyFormatter(): DecimalFormat {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US) as DecimalFormat
        formatter.currency = Currency.getInstance("KES")
        val symbols = formatter.decimalFormatSymbols
        symbols.currencySymbol = "KES "
        formatter.decimalFormatSymbols = symbols
        return formatter
    }

    fun formatCurrency(decimal: BigDecimal): String {
        return buildCurrencyFormatter().format(decimal)
    }

    fun pluralize(text: String, quantity: BigDecimal): String {
        return if (quantity.compareTo(BigDecimal.ONE) == 0) {
            text
        } else {
            "${text}s"
        }
    }
}
