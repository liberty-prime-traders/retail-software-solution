package me.ezra_home.retail_software_solution.util.business

import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.math.BigDecimal

// Serializable because request DTOs that hold a typed decimal can be cached.
// A subclass validates in its own `of` and never compares across types.
abstract class TypedDecimal<SELF : TypedDecimal<SELF>>(private val typedValue: BigDecimal) : Comparable<SELF>, Serializable {

    @JsonValue
    fun toBigDecimal(): BigDecimal = typedValue

    override fun compareTo(other: SELF): Int = typedValue.compareTo(other.toBigDecimal())

    override fun equals(other: Any?): Boolean =
        other is TypedDecimal<*> && javaClass == other.javaClass && typedValue.compareTo(other.toBigDecimal()) == 0

    override fun hashCode(): Int = typedValue.stripTrailingZeros().hashCode()

    override fun toString(): String = typedValue.toPlainString()
}
