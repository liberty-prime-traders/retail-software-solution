package me.ezra_home.retail_software_solution.util.business

import java.math.BigDecimal

data class ConversionRatio(val numerator: Long, val denominator: Long) {

    init {
        require(numerator > 0) { "numerator must be positive" }
        require(denominator > 0) { "denominator must be positive" }
    }

    fun factor(): BigDecimal = Decimals.Scale10.divide(numerator.toBigDecimal(), denominator.toBigDecimal())

    fun displayFactor(): BigDecimal = Decimals.Scale4.round(factor())

    fun applyTo(quantity: BigDecimal): BigDecimal =
        Decimals.Scale10.divide(quantity.multiply(numerator.toBigDecimal()), denominator.toBigDecimal())

    // Money has two decimals fewer than a quantity needs, so a converted price must not carry quantity precision.
    fun applyToPrice(price: BigDecimal): BigDecimal =
        Decimals.Scale4.divide(price.multiply(numerator.toBigDecimal()), denominator.toBigDecimal())

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
