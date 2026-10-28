package me.ezra_home.retail_software_solution.util.quantity

import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal
import java.math.RoundingMode

// Never build one from a number that was already converted or rounded
class ExactQuantity private constructor(private val storageValue: BigDecimal) : Comparable<ExactQuantity> {

    operator fun plus(other: ExactQuantity): ExactQuantity = ExactQuantity(storageValue.add(other.storageValue))
    operator fun minus(other: ExactQuantity): ExactQuantity = ExactQuantity(storageValue.subtract(other.storageValue))

    fun isPositive(): Boolean = storageValue.signum() > 0
    fun isZero(): Boolean = storageValue.signum() == 0
    fun toStorageQuantity(): BigDecimal = storageValue.setScale(STORAGE_SCALE, RoundingMode.UNNECESSARY)

    fun toDisplayQuantity(storageUnitsPerDisplayUnit: Long, roundingMode: RoundingMode): BigDecimal {
        require(storageUnitsPerDisplayUnit >= 1) { "storageUnitsPerDisplayUnit must be at least 1" }
        return storageValue.divide(BigDecimal.valueOf(storageUnitsPerDisplayUnit), DISPLAY_SCALE, roundingMode)
    }

    override fun compareTo(other: ExactQuantity): Int = storageValue.compareTo(other.storageValue)
    override fun equals(other: Any?): Boolean = other is ExactQuantity && storageValue.compareTo(other.storageValue) == 0
    override fun hashCode(): Int = storageValue.stripTrailingZeros().hashCode()
    override fun toString(): String = storageValue.toPlainString()

    companion object {
        val STORAGE_SCALE = Decimals.Scale4.scale
        val DISPLAY_SCALE = Decimals.Scale4.scale

        val ZERO = ExactQuantity(BigDecimal.ZERO)

        fun ofStorageFromUserProvided(positiveUserProvidedQuantity: PositiveUserProvidedQuantity, storageUnitsPerUserProvidedUnit: Long): ExactQuantity =
            ofStorageFromUserProvided(positiveUserProvidedQuantity.toBigDecimal(), storageUnitsPerUserProvidedUnit)

        fun ofStorageFromUserProvided(userProvidedQuantity: BigDecimal, storageUnitsPerUserProvidedUnit: Long): ExactQuantity {
            require(storageUnitsPerUserProvidedUnit >= 1) { "storageUnitsPerUserProvidedUnit must be at least 1" }
            return ExactQuantity(userProvidedQuantity.multiply(BigDecimal.valueOf(storageUnitsPerUserProvidedUnit)))
        }

        fun ofStorage(storageQuantity: BigDecimal): ExactQuantity = ExactQuantity(storageQuantity)
    }
}
