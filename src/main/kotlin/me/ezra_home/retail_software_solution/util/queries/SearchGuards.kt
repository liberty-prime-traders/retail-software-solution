package me.ezra_home.retail_software_solution.util.queries

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal
import java.time.OffsetDateTime

object SearchGuards {

  private const val MAX_RANGE_YEARS = 1L

  fun guardPageSize(requestedSize: Int, minPageSize: Int, maxPageSize: Int) {
    if (requestedSize !in minPageSize..maxPageSize) {
      throw RtsGenericException("requestedSize must be between $minPageSize and $maxPageSize")
    }
  }

  fun guardMaxSize(size: Int, maxSize: Int, label: String) {
    if (size > maxSize) {
      throw RtsGenericException("A maximum of $maxSize $label may be supplied")
    }
  }

  fun guardBoundedRange(from: OffsetDateTime?, before: OffsetDateTime?, fromLabel: String, beforeLabel: String) {
    if (from == null || before == null) {
      throw RtsGenericException("$fromLabel and $beforeLabel are required")
    }
    if (before.isAfter(from.plusYears(MAX_RANGE_YEARS))) {
      throw RtsGenericException("$fromLabel and $beforeLabel must be at most $MAX_RANGE_YEARS year apart")
    }
  }

  fun guardNonNegative(value: BigDecimal?, label: String) {
    if (value != null && value < BigDecimal.ZERO) {
      throw RtsGenericException("$label must not be negative")
    }
  }

  // isOutOfOrder is explicit, not Comparable — OffsetDateTime.isAfter is instant-only and disagrees with compareTo's local-field tie-break.
  fun <T> guardRangeOrder(from: T?, before: T?, message: String, isOutOfOrder: (T, T) -> Boolean) {
    if (from != null && before != null && isOutOfOrder(from, before)) {
      throw RtsGenericException(message)
    }
  }
}
