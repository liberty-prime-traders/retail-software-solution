package me.ezra_home.retail_software_solution.locations.business.sale.search

// Duplicates SaleEntity's Kotlin helpers (discountTotal / surchargeTotal / displaySubtotal / receivableTotal).
// A new surcharge or discount bucket must be added in both places or the grid and the filter will
// silently disagree.
object SaleSearchExpressions {
  private const val SALE_ALIAS = SaleSearchAliases.SALE

  const val DISCOUNT_TOTAL =
    "($SALE_ALIAS.line_level_discount_total + $SALE_ALIAS.order_level_discount_total)"

  const val SURCHARGE_TOTAL =
    "($SALE_ALIAS.line_level_surcharge_total + $SALE_ALIAS.order_level_surcharge_total)"

  const val DISPLAY_SUBTOTAL =
    "($SALE_ALIAS.subtotal + $SURCHARGE_TOTAL)"

  const val TAXABLE_AMOUNT =
    "($DISPLAY_SUBTOTAL - $DISCOUNT_TOTAL)"

  const val RECEIVABLE_TOTAL =
    "($TAXABLE_AMOUNT + $SALE_ALIAS.tax_billed)"
}
