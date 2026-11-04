package me.ezra_home.retail_software_solution.locations.business.product_purchases_report

import me.ezra_home.retail_software_solution.util.queries.SearchGuards
import java.time.OffsetDateTime

object ProductPurchasesReportValidator {

  private const val MIN_PAGE_SIZE = 1
  private const val MAX_PAGE_SIZE = 500

  fun resolveDeliveredRange(deliveredFrom: OffsetDateTime?, deliveredBefore: OffsetDateTime?): ProductPurchasesDeliveredRange {
    SearchGuards.guardBoundedRange(deliveredFrom, deliveredBefore, "deliveredFrom", "deliveredBefore")
    SearchGuards.guardRangeOrder(
      deliveredFrom,
      deliveredBefore,
      "deliveredFrom must not be after deliveredBefore"
    ) { fromValue, beforeValue -> fromValue.isAfter(beforeValue) }
    return ProductPurchasesDeliveredRange(deliveredFrom!!, deliveredBefore!!)
  }

  fun guardValidPageSize(requestedSize: Int) {
    SearchGuards.guardPageSize(requestedSize, MIN_PAGE_SIZE, MAX_PAGE_SIZE)
  }
}
