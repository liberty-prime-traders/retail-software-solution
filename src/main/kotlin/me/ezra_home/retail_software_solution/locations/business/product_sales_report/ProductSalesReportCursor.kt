package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSortDirection
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSortField
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal
import java.util.Base64
import java.util.UUID

// Position is the last row's full ordering key, not just its value, so a page can resume in a recomputed
// snapshot even when the last row itself is gone from it. The sort is part of the cursor because a key
// only has a position within the order it was built for.
data class ProductSalesReportCursor(
  val lastRowKey: ProductSalesRowKey,
  val sortField: ProductSalesSortField,
  val sortDirection: ProductSalesSortDirection
) {

  fun encode(): String {
    val raw = listOf(
      lastRowKey.tier,
      lastRowKey.number.toPlainString(),
      URL_ENCODER.encodeToString(lastRowKey.text.toByteArray()),
      URL_ENCODER.encodeToString(lastRowKey.lowercaseProductName.toByteArray()),
      lastRowKey.id,
      sortField.name,
      sortDirection.name
    ).joinToString(SEPARATOR)
    return Base64.getEncoder().encodeToString(raw.toByteArray())
  }

  companion object {
    private const val SEPARATOR = "|"
    private const val FIELD_COUNT = 7
    private val URL_ENCODER = Base64.getUrlEncoder()
    private val URL_DECODER = Base64.getUrlDecoder()

    fun decode(cursor: String, sortField: ProductSalesSortField, sortDirection: ProductSalesSortDirection): ProductSalesReportCursor? {
      if (cursor.isBlank()) return null
      try {
        val parts = String(Base64.getDecoder().decode(cursor)).split(SEPARATOR)
        if (parts.size != FIELD_COUNT) throw IllegalArgumentException()
        val decoded = ProductSalesReportCursor(
          lastRowKey = ProductSalesRowKey(
            tier = parts[0].toInt(),
            number = BigDecimal(parts[1]),
            text = String(URL_DECODER.decode(parts[2])),
            lowercaseProductName = String(URL_DECODER.decode(parts[3])),
            id = UUID.fromString(parts[4])
          ),
          sortField = ProductSalesSortField.valueOf(parts[5]),
          sortDirection = ProductSalesSortDirection.valueOf(parts[6])
        )
        if (decoded.sortField != sortField || decoded.sortDirection != sortDirection) throw IllegalArgumentException()
        return decoded
      } catch (_: IllegalArgumentException) {
        throw RtsGenericException("Malformed cursor")
      }
    }
  }
}
