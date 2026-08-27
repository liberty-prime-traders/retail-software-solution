package me.ezra_home.retail_software_solution.util.business

import java.math.BigDecimal

data class ConversionRatio(val numerator: Long, val denominator: Long) {

    init {
        require(numerator > 0) { "numerator must be positive" }
        require(denominator > 0) { "denominator must be positive" }
    }

    fun factor(): BigDecimal = Decimals.divideScale4(numerator.toBigDecimal(), denominator.toBigDecimal())

    fun applyTo(quantity: BigDecimal): BigDecimal =
        Decimals.divideScale4(quantity.multiply(numerator.toBigDecimal()), denominator.toBigDecimal())

    fun isEquivalentTo(other: ConversionRatio): Boolean = numerator * other.denominator == other.numerator * denominator

    fun invert(): ConversionRatio = ConversionRatio(denominator, numerator).reduced()

    fun times(other: ConversionRatio): ConversionRatio =
        ConversionRatio(numerator * other.numerator, denominator * other.denominator).reduced()

    fun reduced(): ConversionRatio {
        val g = Decimals.gcd(numerator, denominator)
        return ConversionRatio(numerator / g, denominator / g)
    }

    companion object {
        val IDENTITY = ConversionRatio(1L, 1L)
    }
}
