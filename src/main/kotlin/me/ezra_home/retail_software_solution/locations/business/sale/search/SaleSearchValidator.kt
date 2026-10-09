package me.ezra_home.retail_software_solution.locations.business.sale.search

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSearchParameters
import me.ezra_home.retail_software_solution.util.queries.SearchGuards

object SaleSearchValidator {

  private const val MAX_SALE_REFERENCE_NUMBERS = 20
  private const val MAX_CONTACT_IDS = 100
  private const val MAX_SOLD_BY_USER_IDS = 100
  private const val MIN_PAGE_SIZE = 1
  private const val MAX_PAGE_SIZE = 500

  fun guardValidParameters(saleSearchParameters: SaleSearchParameters) {
    SearchGuards.guardBoundedRange(
      saleSearchParameters.createdFrom,
      saleSearchParameters.createdBefore,
      "createdFrom",
      "createdBefore"
    )
    SearchGuards.guardMaxSize(saleSearchParameters.saleReferenceNumbers.size, MAX_SALE_REFERENCE_NUMBERS, "sale reference numbers")
    SearchGuards.guardMaxSize(saleSearchParameters.contactIds.size, MAX_CONTACT_IDS, "contact ids")
    SearchGuards.guardMaxSize(saleSearchParameters.soldByUserIds.size, MAX_SOLD_BY_USER_IDS, "sold by user ids")

    SearchGuards.guardRangeOrder(
      saleSearchParameters.createdFrom,
      saleSearchParameters.createdBefore,
      "createdFrom must not be after createdBefore"
    ) { from, before -> from.isAfter(before) }

    SearchGuards.guardRangeOrder(
      saleSearchParameters.minReceivableTotal,
      saleSearchParameters.maxReceivableTotal,
      "minReceivableTotal must not be greater than maxReceivableTotal"
    ) { min, max -> min > max }

    SearchGuards.guardRangeOrder(
      saleSearchParameters.minDiscountTotal,
      saleSearchParameters.maxDiscountTotal,
      "minDiscountTotal must not be greater than maxDiscountTotal"
    ) { min, max -> min > max }

    SearchGuards.guardNonNegative(saleSearchParameters.minReceivableTotal, "minReceivableTotal")
    SearchGuards.guardNonNegative(saleSearchParameters.maxReceivableTotal, "maxReceivableTotal")
    SearchGuards.guardNonNegative(saleSearchParameters.minDiscountTotal, "minDiscountTotal")
    SearchGuards.guardNonNegative(saleSearchParameters.maxDiscountTotal, "maxDiscountTotal")
  }

  fun guardValidPageSize(requestedSize: Int) {
    SearchGuards.guardPageSize(requestedSize, MIN_PAGE_SIZE, MAX_PAGE_SIZE)
  }
}
