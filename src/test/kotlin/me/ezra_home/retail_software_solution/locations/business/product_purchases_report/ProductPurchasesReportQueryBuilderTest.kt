package me.ezra_home.retail_software_solution.locations.business.product_purchases_report

import me.ezra_home.retail_software_solution.locations.business.product_purchases_report.api.ProductPurchasesDeliveryLinesParameters
import me.ezra_home.retail_software_solution.locations.business.product_purchases_report.api.ProductPurchasesReportParameters
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.queries.QueryParameterNames
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class ProductPurchasesReportQueryBuilderTest {

  private val range = ProductPurchasesDeliveredRange(
    OffsetDateTime.parse("2026-10-01T00:00:00+00:00"),
    OffsetDateTime.parse("2026-10-08T00:00:00+00:00")
  )
  private val locationProductId = UUID.randomUUID()

  @Test
  fun `summary counts only applied deliveries and active lines`() {
    val context = ProductPurchasesReportQueryBuilder.buildPredicate(
      ProductPurchasesReportParameters(range.fromInclusive, range.beforeExclusive),
      range
    )

    assertTrue(context.whereClauses.contains("pd.status = ANY(:appliedDeliveryStatuses)"))
    assertTrue(context.whereClauses.contains("pdl.status = :activeLineStatus"))
    assertEquals(listOf("R", "PRV", "FRV"), (context.params["appliedDeliveryStatuses"] as Array<*>).toList())
    assertEquals("ACT", context.params["activeLineStatus"])
  }

  @Test
  fun `expansion applies the same header filter but not the active line filter`() {
    val sqlQuery = ProductPurchasesReportQueryBuilder.buildDeliveryLinesQuery(
      locationProductId,
      ProductPurchasesDeliveryLinesParameters(range.fromInclusive, range.beforeExclusive),
      range,
      null
    )

    assertTrue(sqlQuery.sql.contains("pd.status = ANY(:appliedDeliveryStatuses)"))
    assertFalse(sqlQuery.sql.contains("activeLineStatus"))
    assertFalse(sqlQuery.params.containsKey(QueryParameterNames.CURSOR_ID))
  }

  @Test
  fun `expansion adds a keyset condition when a cursor is given`() {
    val cursor = KeysetSearchCursor(range.beforeExclusive, UUID.randomUUID())

    val sqlQuery = ProductPurchasesReportQueryBuilder.buildDeliveryLinesQuery(
      locationProductId,
      ProductPurchasesDeliveryLinesParameters(range.fromInclusive, range.beforeExclusive),
      range,
      cursor
    )

    assertTrue(sqlQuery.sql.contains("(pd.delivered_at, pdl.id) <"))
    assertEquals(cursor.id, sqlQuery.params[QueryParameterNames.CURSOR_ID])
  }
}
