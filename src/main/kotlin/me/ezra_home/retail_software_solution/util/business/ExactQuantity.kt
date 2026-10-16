package me.ezra_home.retail_software_solution.util.business

import java.math.BigDecimal
import java.math.BigInteger

class ExactQuantity private constructor(
    private val numerator: BigInteger,
    private val denominator: BigInteger,
) : Comparable<ExactQuantity> {

    operator fun plus(other: ExactQuantity): ExactQuantity =
        of(numerator * other.denominator + other.numerator * denominator, denominator * other.denominator)

    operator fun minus(other: ExactQuantity): ExactQuantity =
        of(numerator * other.denominator - other.numerator * denominator, denominator * other.denominator)

    fun coerceAtLeastZero(): ExactQuantity = if (numerator.signum() < 0) ZERO else this

    fun isPositive(): Boolean = numerator.signum() > 0

    fun toDecimal(): BigDecimal = Decimals.Scale10.divide(BigDecimal(numerator), BigDecimal(denominator))

    // Dividing by the ratio exactly and rounding once keeps 26 pieces at 1/18 as 26, not 25.9992 via a rounded 1.4444.
    fun toDecimalIn(conversionRatio: ConversionRatio): BigDecimal =
        Decimals.Scale4.divide(
            BigDecimal(numerator * BigInteger.valueOf(conversionRatio.denominator)),
            BigDecimal(denominator * BigInteger.valueOf(conversionRatio.numerator)),
        )

    override fun compareTo(other: ExactQuantity): Int = (numerator * other.denominator).compareTo(other.numerator * denominator)

    override fun equals(other: Any?): Boolean = other is ExactQuantity && numerator == other.numerator && denominator == other.denominator

    override fun hashCode(): Int = 31 * numerator.hashCode() + denominator.hashCode()

    companion object {
        val ZERO = ExactQuantity(BigInteger.ZERO, BigInteger.ONE)

        fun of(quantity: BigDecimal, conversionRatio: ConversionRatio): ExactQuantity {
            val scale = quantity.scale()
            val scaledNumerator = quantity.unscaledValue() * BigInteger.valueOf(conversionRatio.numerator)
            val scaledDenominator = BigInteger.valueOf(conversionRatio.denominator)
            return if (scale >= 0) of(scaledNumerator, scaledDenominator * BigInteger.TEN.pow(scale))
            else of(scaledNumerator * BigInteger.TEN.pow(-scale), scaledDenominator)
        }

        private fun of(numerator: BigInteger, denominator: BigInteger): ExactQuantity {
            val divisor = numerator.gcd(denominator).takeIf { it.signum() != 0 } ?: BigInteger.ONE
            return ExactQuantity(numerator / divisor, denominator / divisor)
        }
    }
}
