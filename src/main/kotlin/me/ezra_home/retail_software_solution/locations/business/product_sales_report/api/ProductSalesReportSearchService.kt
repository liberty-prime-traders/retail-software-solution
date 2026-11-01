package me.ezra_home.retail_software_solution.locations.business.product_sales_report.api

import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportCursor
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportKeyedRow
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportSnapshot
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportSnapshotBuilder
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportSnapshotCache
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportValidator
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportWindow
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponseWithMetadata
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ProductSalesReportSearchService(
  private val productSalesReportSnapshotBuilder: ProductSalesReportSnapshotBuilder,
  private val productSalesReportSnapshotCache: ProductSalesReportSnapshotCache,
  // Shared with product search. It is compared against the number of products with sales in the range, which can
  // never exceed the number of products, so the threshold still caps the rows returned in one response.
  @param:Value($$"${rts.product-search.client-side-filter-threshold}")
  private val clientSideFilterThreshold: Long
) {

  fun search(pageRequest: PageRequest<ProductSalesReportSearchParameters, String>): PageResponseWithMetadata<ProductSalesSummaryDto, String, ProductSalesReportSearchMetadata> {
    val productSalesReportSearchParameters = pageRequest.parameters
    val window = ProductSalesReportWindow.ofInclusiveDates(
      productSalesReportSearchParameters.from,
      productSalesReportSearchParameters.to
    )
    val sortField = productSalesReportSearchParameters.sortField
    val sortDirection = productSalesReportSearchParameters.sortDirection
    ProductSalesReportValidator.guardValidSort(sortField, sortDirection)
    val productSalesReportCursor = ProductSalesReportCursor.decode(pageRequest.previousCursor, sortField, sortDirection)

    val snapshot = productSalesReportSnapshotCache.getOrBuild(window) { productSalesReportSnapshotBuilder.build(window) }
    val metadata = ProductSalesReportSearchMetadata(snapshot.builtAt)

    if (productSalesReportCursor == null && snapshot.rows.size <= clientSideFilterThreshold) {
      return PageResponseWithMetadata(
        currentCursor = "",
        hasMore = false,
        contents = snapshot.orderedRows(sortField, sortDirection).map { it.row.summary },
        requireClientSideFilter = true,
        metadata = metadata
      )
    }

    ProductSalesReportValidator.guardValidPageSize(pageRequest.requestedSize)
    return page(pageRequest, productSalesReportSearchParameters, productSalesReportCursor, snapshot, metadata)
  }

  private fun page(
    pageRequest: PageRequest<ProductSalesReportSearchParameters, String>,
    productSalesReportSearchParameters: ProductSalesReportSearchParameters,
    productSalesReportCursor: ProductSalesReportCursor?,
    snapshot: ProductSalesReportSnapshot,
    metadata: ProductSalesReportSearchMetadata
  ): PageResponseWithMetadata<ProductSalesSummaryDto, String, ProductSalesReportSearchMetadata> {
    val sortField = productSalesReportSearchParameters.sortField
    val sortDirection = productSalesReportSearchParameters.sortDirection
    val comparableSearchText = productSalesReportSearchParameters.searchText
      ?.let { StringUtils.normalizeForComparison(it) }
      ?.takeIf { it.isNotEmpty() }

    val rowsAfterCursor = productSalesReportCursor
      ?.let { snapshot.rowsAfter(sortField, sortDirection, it.lastRowKey) }
      ?: snapshot.orderedRows(sortField, sortDirection)
    val candidateRows = rowsAfterCursor.asSequence()
      .filter { comparableSearchText == null || matches(it, comparableSearchText) }
      .take(pageRequest.requestedSize + 1)
      .toList()

    val pageRows = candidateRows.take(pageRequest.requestedSize)
    val currentCursor = pageRows.lastOrNull()
      ?.let { ProductSalesReportCursor(it.key, sortField, sortDirection).encode() }
      ?: pageRequest.previousCursor

    return PageResponseWithMetadata(
      currentCursor = currentCursor,
      hasMore = candidateRows.size > pageRequest.requestedSize,
      contents = pageRows.map { it.row.summary },
      metadata = metadata
    )
  }

  private fun matches(keyedRow: ProductSalesReportKeyedRow, comparableSearchText: String): Boolean =
    keyedRow.row.comparableProductName.contains(comparableSearchText)
      || keyedRow.row.comparableProductGroupName.contains(comparableSearchText)
}
