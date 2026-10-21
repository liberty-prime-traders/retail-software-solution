package me.ezra_home.retail_software_solution.util.quantity

import java.math.BigDecimal
import java.math.RoundingMode

// Never build one from a number that was already converted or rounded
class ExactQuantity private constructor(private val storageValue: BigDecimal) : Comparable<ExactQuantity> {

    operator fun plus(other: ExactQuantity): ExactQuantity = ExactQuantity(storageValue.add(other.storageValue))

    operator fun minus(other: ExactQuantity): ExactQuantity = ExactQuantity(storageValue.subtract(other.storageValue))

    fun isPositive(): Boolean = storageValue.signum() > 0

    fun isZero(): Boolean = storageValue.signum() == 0

    fun toStorageQuantity(): BigDecimal = storageValue.setScale(STORAGE_SCALE, RoundingMode.UNNECESSARY)

    fun inUnitOfSize(unitSizeInRoot: Long, roundingMode: RoundingMode): BigDecimal =
        storageValue.divide(BigDecimal.valueOf(unitSizeInRoot), DISPLAY_SCALE, roundingMode)

    override fun compareTo(other: ExactQuantity): Int = storageValue.compareTo(other.storageValue)

    override fun equals(other: Any?): Boolean = other is ExactQuantity && storageValue.compareTo(other.storageValue) == 0

    override fun hashCode(): Int = storageValue.stripTrailingZeros().hashCode()

    override fun toString(): String = storageValue.toPlainString()

    companion object {
        const val STORAGE_SCALE = 4
        const val DISPLAY_SCALE = 4

        val ZERO = ExactQuantity(BigDecimal.ZERO)

        fun ofUserProvided(userProvidedQuantity: BigDecimal, storageUnitsPerUnit: Long): ExactQuantity {
            require(storageUnitsPerUnit >= 1) { "storageUnitsPerUnit must be at least 1" }
            return ExactQuantity(userProvidedQuantity.multiply(BigDecimal.valueOf(storageUnitsPerUnit)))
        }

        fun ofStorage(storageQuantity: BigDecimal): ExactQuantity = ExactQuantity(storageQuantity)
    }
}
