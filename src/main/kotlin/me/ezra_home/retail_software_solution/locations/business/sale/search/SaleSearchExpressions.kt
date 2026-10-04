package me.ezra_home.retail_software_solution.locations.business.sale.search

// Duplicates SaleEntity's Kotlin helpers (discountTotal / surchargeTotal / displaySubtotal / receivableTotal).
// A new surcharge or discount bucket must be added in both places or the grid and the filter will
// silently disagree.
object SaleSearchExpressions {
  private const val S = SaleSearchAliases.SALE

  const val DISCOUNT_TOTAL =
    "(COALESCE($S.line_level_discount_total, 0) + COALESCE($S.order_level_discount_total, 0))"

  const val SURCHARGE_TOTAL =
    "(COALESCE($S.line_level_surcharge_total, 0) + COALESCE($S.order_level_surcharge_total, 0))"

  const val DISPLAY_SUBTOTAL =
    "(COALESCE($S.subtotal, 0) + $SURCHARGE_TOTAL)"

  const val TAXABLE_AMOUNT =
    "($DISPLAY_SUBTOTAL - $DISCOUNT_TOTAL)"

  const val RECEIVABLE_TOTAL =
    "($TAXABLE_AMOUNT + COALESCE($S.tax_billed, 0))"
}
