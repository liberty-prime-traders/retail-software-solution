package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSortDirection
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSortField
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.Base64
import java.util.UUID

class ProductSalesReportCursorTest {

  private val sortField = ProductSalesSortField.NET_SALES
  private val sortDirection = ProductSalesSortDirection.ASC

  private fun cursor(number: BigDecimal, productName: String = "pipe | wrench") = ProductSalesReportCursor(
    lastRowKey = ProductSalesRowKey(0, number, "text | value", productName, UUID.randomUUID()),
    sortField = sortField,
    sortDirection = sortDirection
  )

  private fun decode(encoded: String) = ProductSalesReportCursor.decode(encoded, sortField, sortDirection)

  @Test
  fun `a cursor round-trips including a separator inside the product name and the text`() {
    val original = cursor(BigDecimal("12.5"))

    assertEquals(original, decode(original.encode()))
  }

  @Test
  fun `a blank cursor means the first page`() {
    assertNull(decode(""))
  }

  @Test
  fun `garbage and truncated cursors are rejected`() {
    listOf("not-base64!!", Base64.getEncoder().encodeToString("a|b".toByteArray())).forEach {
      assertEquals("Malformed cursor", assertThrows<RtsGenericException> { decode(it) }.message)
    }
  }

  @Test
  fun `a tampered field is rejected`() {
    val parts = String(Base64.getDecoder().decode(cursor(BigDecimal.ONE).encode())).split("|")
    listOf(0 to "x", 1 to "NaN", 4 to "not-a-uuid", 5 to "NOPE", 6 to "SIDEWAYS").forEach { (index, tamperedValue) ->
      val tampered = parts.toMutableList().also { it[index] = tamperedValue }
      assertThrows<RtsGenericException> { decode(Base64.getEncoder().encodeToString(tampered.joinToString("|").toByteArray())) }
    }
  }

  @Test
  fun `a cursor issued for another sort field or direction is rejected`() {
    val encoded = cursor(BigDecimal.TEN).encode()

    assertThrows<RtsGenericException> { ProductSalesReportCursor.decode(encoded, ProductSalesSortField.COGS, sortDirection) }
    assertThrows<RtsGenericException> { ProductSalesReportCursor.decode(encoded, sortField, ProductSalesSortDirection.DESC) }
  }
}
