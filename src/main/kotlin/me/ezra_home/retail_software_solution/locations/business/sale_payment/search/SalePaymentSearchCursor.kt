package me.ezra_home.retail_software_solution.locations.business.sale_payment.search

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

data class SalePaymentSearchCursor(val createdOn: OffsetDateTime, val id: UUID) {

  fun encode(): String {
    val raw = "$createdOn|$id"
    return Base64.getEncoder().encodeToString(raw.toByteArray())
  }

  companion object {
    fun decode(cursor: String): SalePaymentSearchCursor? {
      if (cursor.isBlank()) return null
      try {
        val raw = String(Base64.getDecoder().decode(cursor))
        val parts = raw.split("|")
        if (parts.size != 2) throw IllegalArgumentException()
        return SalePaymentSearchCursor(OffsetDateTime.parse(parts[0]), UUID.fromString(parts[1]))
      } catch (_: Exception) {
        throw RtsGenericException("Malformed cursor")
      }
    }
  }
}
