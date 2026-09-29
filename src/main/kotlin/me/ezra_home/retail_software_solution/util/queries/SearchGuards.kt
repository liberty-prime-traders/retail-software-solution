package me.ezra_home.retail_software_solution.util.queries

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException

object SearchGuards {

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

  // isOutOfOrder is explicit, not Comparable — OffsetDateTime.isAfter is instant-only and disagrees with compareTo's local-field tie-break.
  fun <T> guardRangeOrder(from: T?, before: T?, message: String, isOutOfOrder: (T, T) -> Boolean) {
    if (from != null && before != null && isOutOfOrder(from, before)) {
      throw RtsGenericException(message)
    }
  }
}
