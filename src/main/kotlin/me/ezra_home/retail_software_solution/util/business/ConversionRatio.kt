package me.ezra_home.retail_software_solution.util.business

import java.math.BigDecimal

data class ConversionRatio(val numerator: Long, val denominator: Long) {

    init {
        require(numerator > 0) { "numerator must be positive" }
        require(denominator > 0) { "denominator must be positive" }
    }

    fun factor(): BigDecimal = Decimals.Scale10.divide(BigDecimal.valueOf(numerator), BigDecimal.valueOf(denominator))

    fun reduced(): ConversionRatio {
        val g = Decimals.gcd(numerator, denominator)
        return ConversionRatio(numerator / g, denominator / g)
    }

    fun <T> buildFromParts(builder: (numerator: Long, denominator: Long) -> T): T = builder(numerator, denominator)

    companion object {
        val IDENTITY = ConversionRatio(1L, 1L)
    }
}
