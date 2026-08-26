package me.ezra_home.retail_software_solution.util.business

import java.math.BigDecimal

data class ConversionRatio(val numerator: Long, val denominator: Long) {

    init {
        require(denominator != 0L) { "denominator must not be zero" }
    }

    fun factor(): BigDecimal = Decimals.divideScale4(numerator.toBigDecimal(), denominator.toBigDecimal())

    fun applyTo(quantity: BigDecimal): BigDecimal =
        Decimals.divideScale4(quantity.multiply(numerator.toBigDecimal()), denominator.toBigDecimal())

    fun invert(): ConversionRatio = ConversionRatio(denominator, numerator).reduced()

    fun times(other: ConversionRatio): ConversionRatio =
        ConversionRatio(numerator * other.numerator, denominator * other.denominator).reduced()

    fun reduced(): ConversionRatio {
        val sign = if (denominator < 0) -1L else 1L
        val g = Decimals.gcd(numerator, denominator)
        return ConversionRatio(sign * numerator / g, sign * denominator / g)
    }

    companion object {
        val IDENTITY = ConversionRatio(1L, 1L)
    }
}
