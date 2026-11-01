package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSortDirection
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSortField
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSummaryDto
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap

// builtAt is the age of the oldest day bucket behind the rows, not the time the rows were combined.
class ProductSalesReportSnapshot(val builtAt: OffsetDateTime, val rows: List<ProductSalesReportSnapshotRow>) {

  private data class SortOrder(val sortField: ProductSalesSortField, val sortDirection: ProductSalesSortDirection)

  private val orderedRowsBySortOrder = ConcurrentHashMap<SortOrder, List<ProductSalesReportKeyedRow>>()

  // Each order is computed once per snapshot, so paging a sort never re-sorts.
  fun orderedRows(sortField: ProductSalesSortField, sortDirection: ProductSalesSortDirection): List<ProductSalesReportKeyedRow> =
    orderedRowsBySortOrder.computeIfAbsent(SortOrder(sortField, sortDirection)) {
      val comparator = ProductSalesReportOrdering.comparator(sortDirection)
      rows.map { ProductSalesReportKeyedRow(it, ProductSalesReportOrdering.keyOf(it.summary, sortField)) }
        .sortedWith { first, second -> comparator.compare(first.key, second.key) }
    }

  // A view, not a copy. Works when lastRowKey itself is gone from rows, so a cursor survives a rebuilt snapshot.
  fun rowsAfter(
    sortField: ProductSalesSortField,
    sortDirection: ProductSalesSortDirection,
    lastRowKey: ProductSalesRowKey
  ): List<ProductSalesReportKeyedRow> {
    val orderedRows = orderedRows(sortField, sortDirection)
    val comparator = ProductSalesReportOrdering.comparator(sortDirection)
    val position = orderedRows.binarySearch { comparator.compare(it.key, lastRowKey) }
    val firstIndexAfter = if (position >= 0) position + 1 else -(position + 1)
    return orderedRows.subList(firstIndexAfter, orderedRows.size)
  }
}

// The comparison forms are computed once per snapshot so a search normalizes only its own text.
data class ProductSalesReportSnapshotRow(
  val summary: ProductSalesSummaryDto,
  val comparableProductName: String,
  val comparableProductGroupName: String
)

data class ProductSalesReportKeyedRow(val row: ProductSalesReportSnapshotRow, val key: ProductSalesRowKey)
