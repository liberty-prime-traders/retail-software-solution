package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSortDirection
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSortField
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSummaryDto
import java.math.BigDecimal
import java.util.Locale
import java.util.UUID

// A sort key is tier, then number, then text, then the product's name and id as the final tie-break. Which
// of number and text carries the sorted value depends on the sort field. Under NEEDS_ATTENTION the number is
// oriented so a larger one ranks first within its tier: the loss tier holds the negated gross profit.
data class ProductSalesRowKey(
  val tier: Int,
  val number: BigDecimal,
  val text: String,
  val lowercaseProductName: String,
  val id: UUID
)

object ProductSalesReportOrdering {

  private const val LOSS_TIER = 0
  private const val NET_DISCOUNTED_TIER = 1
  private const val OTHER_TIER = 2

  private const val PRESENT_TIER = 0
  private const val ABSENT_TIER = 1

  // The tier is never reversed, so an absent value sorts last in either direction.
  fun comparator(sortDirection: ProductSalesSortDirection): Comparator<ProductSalesRowKey> = Comparator { first, second ->
    val tierComparison = first.tier.compareTo(second.tier)
    if (tierComparison != 0) return@Comparator tierComparison
    val numberComparison = first.number.compareTo(second.number)
    if (numberComparison != 0) return@Comparator oriented(numberComparison, sortDirection)
    val textComparison = first.text.compareTo(second.text)
    if (textComparison != 0) return@Comparator oriented(textComparison, sortDirection)
    val nameComparison = first.lowercaseProductName.compareTo(second.lowercaseProductName)
    if (nameComparison != 0) nameComparison else first.id.compareTo(second.id)
  }

  fun keyOf(summary: ProductSalesSummaryDto, sortField: ProductSalesSortField): ProductSalesRowKey = when (sortField) {
    ProductSalesSortField.NEEDS_ATTENTION -> needsAttentionKey(summary)
    ProductSalesSortField.PRODUCT_NAME -> key(summary, PRESENT_TIER, BigDecimal.ZERO, lowercaseProductName(summary))
    ProductSalesSortField.BASE_QUANTITY -> numberKey(summary, summary.baseQuantity)
    ProductSalesSortField.NET_SALES -> numberKey(summary, summary.netSales)
    ProductSalesSortField.COGS -> numberKey(summary, summary.cogs)
    ProductSalesSortField.GROSS_PROFIT -> numberKey(summary, summary.grossProfit)
    ProductSalesSortField.MARGIN_PERCENT -> numberKey(summary, summary.marginPercent)
    ProductSalesSortField.ADJUSTMENTS_PERCENT -> numberKey(summary, summary.adjustmentsPercent)
  }

  // Ascending comparisons are the natural order of the fields; descending flips them.
  private fun oriented(comparison: Int, sortDirection: ProductSalesSortDirection): Int =
    if (sortDirection == ProductSalesSortDirection.ASC) comparison else -comparison

  private fun needsAttentionKey(summary: ProductSalesSummaryDto): ProductSalesRowKey {
    val netDiscountMoney = summary.grossSales - summary.netSales
    return when {
      summary.grossProfit.signum() < 0 -> key(summary, LOSS_TIER, summary.grossProfit.negate())
      netDiscountMoney.signum() > 0 -> key(summary, NET_DISCOUNTED_TIER, netDiscountMoney)
      else -> key(summary, OTHER_TIER, summary.netSales)
    }
  }

  private fun numberKey(summary: ProductSalesSummaryDto, number: BigDecimal?): ProductSalesRowKey =
    if (number == null) key(summary, ABSENT_TIER, BigDecimal.ZERO) else key(summary, PRESENT_TIER, number)

  private fun key(summary: ProductSalesSummaryDto, tier: Int, number: BigDecimal, text: String = ""): ProductSalesRowKey =
    ProductSalesRowKey(tier, number, text, lowercaseProductName(summary), summary.locationProductId)

  private fun lowercaseProductName(summary: ProductSalesSummaryDto): String = summary.productName.lowercase(Locale.ROOT)
}
