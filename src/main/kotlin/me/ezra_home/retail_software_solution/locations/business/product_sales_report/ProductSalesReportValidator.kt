package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSortDirection
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSortField
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.queries.SearchGuards
import java.time.OffsetDateTime
import java.util.UUID

object ProductSalesReportValidator {

  private const val MIN_SUMMARY_PRODUCTS = 1
  private const val MAX_SUMMARY_PRODUCTS = 4
  private const val MIN_PAGE_SIZE = 1
  private const val MAX_PAGE_SIZE = 500

  fun resolveDateRange(from: OffsetDateTime?, before: OffsetDateTime?): DateSoldRange {
    SearchGuards.guardBoundedRange(from, before, "from", "before")
    SearchGuards.guardRangeOrder(from, before, "from must not be after before") { fromValue, beforeValue ->
      fromValue.isAfter(beforeValue)
    }
    return DateSoldRange(from!!, before!!)
  }

  fun guardValidSummaryProductIds(locationProductIds: List<UUID>) {
    if (locationProductIds.size !in MIN_SUMMARY_PRODUCTS..MAX_SUMMARY_PRODUCTS) {
      throw RtsGenericException("locationProductIds must contain between $MIN_SUMMARY_PRODUCTS and $MAX_SUMMARY_PRODUCTS products")
    }
    if (locationProductIds.toSet().size != locationProductIds.size) {
      throw RtsGenericException("locationProductIds must not contain duplicates")
    }
  }

  fun guardValidSort(sortField: ProductSalesSortField, sortDirection: ProductSalesSortDirection) {
    if (sortField == ProductSalesSortField.NEEDS_ATTENTION && sortDirection != ProductSalesSortDirection.DESC) {
      throw RtsGenericException("NEEDS_ATTENTION can only be sorted descending")
    }
  }

  fun guardValidPageSize(requestedSize: Int) {
    SearchGuards.guardPageSize(requestedSize, MIN_PAGE_SIZE, MAX_PAGE_SIZE)
  }
}
