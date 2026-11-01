package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import java.time.OffsetDateTime
import java.util.UUID

data class ProductSalesDayBucket(
  val builtAt: OffsetDateTime,
  val rawRowsByLocationProductId: Map<UUID, ProductSalesSummaryRawRow>
)
