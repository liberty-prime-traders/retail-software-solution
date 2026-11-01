package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportTestSummaries.summary
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSortDirection
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSortField
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSummaryDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class ProductSalesReportOrderingTest {

  private fun ordered(
    summaries: List<ProductSalesSummaryDto>,
    sortField: ProductSalesSortField = ProductSalesSortField.NEEDS_ATTENTION,
    sortDirection: ProductSalesSortDirection = ProductSalesSortDirection.DESC
  ): List<String> {
    val comparator = ProductSalesReportOrdering.comparator(sortDirection)
    return summaries
      .map { it to ProductSalesReportOrdering.keyOf(it, sortField) }
      .sortedWith { first, second -> comparator.compare(first.second, second.second) }
      .map { it.first.productName }
  }

  private val bigLoss = summary("Big loss", grossSales = "100", cogs = "150")
  private val smallLoss = summary("Small loss", grossSales = "100", cogs = "110")
  private val bigDiscount = summary("Big discount", grossSales = "1000", cogs = "100", discount = "200")
  private val smallDiscount = summary("Small discount", grossSales = "1000", cogs = "100", discount = "5")
  private val topSeller = summary("Top seller", grossSales = "500", cogs = "100")
  private val tiedLowerName = summary("alpha", grossSales = "100", cogs = "50", locationProductId = UUID(0, 2))
  private val tiedHigherName = summary("Beta", grossSales = "100", cogs = "50", locationProductId = UUID(0, 1))
  private val zeroSales = summary("Zero sales", grossSales = "0", cogs = "0")

  @Test
  fun `needs attention ranks losses, then net discounted, then the rest by net sales, ties by name`() {
    val all = listOf(tiedHigherName, topSeller, smallDiscount, zeroSales, bigLoss, tiedLowerName, bigDiscount, smallLoss)

    assertEquals(
      listOf("Big loss", "Small loss", "Big discount", "Small discount", "Top seller", "alpha", "Beta", "Zero sales"),
      ordered(all)
    )
  }

  @Test
  fun `a loss-making product ranks in the loss tier even when it was also discounted`() {
    val discountedLoss = summary("Discounted loss", grossSales = "1000", cogs = "900", discount = "300")

    assertEquals(listOf("Discounted loss", "Big discount"), ordered(listOf(bigDiscount, discountedLoss)))
  }

  @Test
  fun `ties on name are broken case-insensitively and then by id`() {
    val sameNameLowerId = summary("Same", grossSales = "100", cogs = "50", locationProductId = UUID(0, 1))
    val sameNameHigherId = summary("same", grossSales = "100", cogs = "50", locationProductId = UUID(0, 2))

    assertEquals(listOf("Same", "same"), ordered(listOf(sameNameHigherId, sameNameLowerId)))
  }

  @Test
  fun `a numeric sort follows the direction and breaks ties by name ascending either way`() {
    val all = listOf(topSeller, tiedHigherName, tiedLowerName)

    assertEquals(
      listOf("Top seller", "alpha", "Beta"),
      ordered(all, ProductSalesSortField.NET_SALES, ProductSalesSortDirection.DESC)
    )
    assertEquals(
      listOf("alpha", "Beta", "Top seller"),
      ordered(all, ProductSalesSortField.NET_SALES, ProductSalesSortDirection.ASC)
    )
  }

  @Test
  fun `a name sort follows the direction`() {
    val all = listOf(topSeller, tiedHigherName, tiedLowerName)

    assertEquals(
      listOf("alpha", "Beta", "Top seller"),
      ordered(all, ProductSalesSortField.PRODUCT_NAME, ProductSalesSortDirection.ASC)
    )
    assertEquals(
      listOf("Top seller", "Beta", "alpha"),
      ordered(all, ProductSalesSortField.PRODUCT_NAME, ProductSalesSortDirection.DESC)
    )
  }

  @Test
  fun `a null percentage sorts last in either direction`() {
    val all = listOf(zeroSales, topSeller, bigLoss)

    assertEquals(
      listOf("Big loss", "Top seller", "Zero sales"),
      ordered(all, ProductSalesSortField.MARGIN_PERCENT, ProductSalesSortDirection.ASC)
    )
    assertEquals(
      listOf("Top seller", "Big loss", "Zero sales"),
      ordered(all, ProductSalesSortField.MARGIN_PERCENT, ProductSalesSortDirection.DESC)
    )
  }
}
