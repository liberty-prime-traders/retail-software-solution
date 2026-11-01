package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSortDirection
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSortField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class ProductSalesReportSnapshotTest {

  private fun row(productName: String, grossSales: String) =
    ProductSalesReportSnapshotRow(ProductSalesReportTestSummaries.summary(productName, grossSales = grossSales, cogs = "1"), "", "")

  private fun keyOf(row: ProductSalesReportSnapshotRow, sortField: ProductSalesSortField = ProductSalesSortField.NEEDS_ATTENTION) =
    ProductSalesReportOrdering.keyOf(row.summary, sortField)

  private val rows = listOf(row("p1", "500"), row("p2", "400"), row("p3", "300"), row("p4", "200"))
  private val snapshot = ProductSalesReportSnapshot(OffsetDateTime.now(), rows)

  private fun rowsAfter(
    lastRowKey: ProductSalesRowKey,
    sortField: ProductSalesSortField = ProductSalesSortField.NEEDS_ATTENTION,
    sortDirection: ProductSalesSortDirection = ProductSalesSortDirection.DESC
  ) = snapshot.rowsAfter(sortField, sortDirection, lastRowKey).map { it.row.summary.productName }

  @Test
  fun `rows after a present key start at the next row`() {
    assertEquals(listOf("p3", "p4"), rowsAfter(keyOf(rows[1])))
  }

  @Test
  fun `rows after a key that left the snapshot start at the first row ordered below it`() {
    assertEquals(listOf("p3", "p4"), rowsAfter(keyOf(row("gone", "350"))))
  }

  @Test
  fun `a key ordered above every row returns all rows and one ordered below every row returns none`() {
    assertEquals(listOf("p1", "p2", "p3", "p4"), rowsAfter(keyOf(row("top", "900"))))
    assertEquals(emptyList<String>(), rowsAfter(keyOf(rows.last())))
    assertEquals(emptyList<String>(), rowsAfter(keyOf(row("bottom", "100"))))
  }

  @Test
  fun `an ascending order resumes after a key in that order`() {
    val ascendingKey = keyOf(rows[2], ProductSalesSortField.NET_SALES)

    assertEquals(
      listOf("p2", "p1"),
      rowsAfter(ascendingKey, ProductSalesSortField.NET_SALES, ProductSalesSortDirection.ASC)
    )
  }

  @Test
  fun `each order is computed once`() {
    assertSame(
      snapshot.orderedRows(ProductSalesSortField.COGS, ProductSalesSortDirection.ASC),
      snapshot.orderedRows(ProductSalesSortField.COGS, ProductSalesSortDirection.ASC)
    )
  }
}
