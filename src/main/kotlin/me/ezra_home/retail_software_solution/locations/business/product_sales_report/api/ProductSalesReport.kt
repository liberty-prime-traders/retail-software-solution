package me.ezra_home.retail_software_solution.locations.business.product_sales_report.api

import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ProductSalesLinesParameters(
  val from: OffsetDateTime? = null,
  val before: OffsetDateTime? = null
)

data class ProductSalesSummaryParameters(
  val locationProductIds: List<UUID> = emptyList(),
  val from: OffsetDateTime? = null,
  val before: OffsetDateTime? = null
)

data class ProductSalesLineAdjustmentDto(
  val reasonName: String,
  val direction: AdjustmentDirection,
  val amount: BigDecimal
)

data class ProductSalesLineDto(
  val id: UUID,
  val saleReferenceNumber: String,
  val dateSold: OffsetDateTime,
  val quantityEntered: String,
  val baseQuantity: BigDecimal,
  val grossSales: BigDecimal,
  val netSales: BigDecimal,
  val cogs: BigDecimal,
  val grossProfit: BigDecimal,
  val marginPercent: BigDecimal?,
  val adjustments: List<ProductSalesLineAdjustmentDto>
)

data class ProductSalesSummaryDto(
  val locationProductId: UUID,
  val orgProductId: UUID,
  val productName: String,
  val productGroupName: String,
  val baseUnitName: String,
  val lineCount: Long,
  val baseQuantity: BigDecimal,
  val baseQuantityText: String,
  val grossSales: BigDecimal,
  val discountTotal: BigDecimal,
  val surchargeTotal: BigDecimal,
  val netSales: BigDecimal,
  val cogs: BigDecimal,
  val grossProfit: BigDecimal,
  val marginPercent: BigDecimal?,
  val marginBeforeAdjustmentsPercent: BigDecimal?,
  val adjustmentsPercent: BigDecimal?,
  val avgListPrice: BigDecimal?,
  val avgSellingPrice: BigDecimal?,
  val avgCost: BigDecimal?,
  val profitPerBaseUnit: BigDecimal?,
  // 1-based needs-attention position across the whole searched range, whatever the page's sort and never
  // renumbered by a search. Null on the summary endpoint.
  val rank: Int? = null
)

// from and to are organization-zone calendar days, both inclusive.
data class ProductSalesReportSearchParameters(
  val from: LocalDate? = null,
  val to: LocalDate? = null,
  val searchText: String? = null,
  val sortField: ProductSalesSortField = ProductSalesSortField.NEEDS_ATTENTION,
  val sortDirection: ProductSalesSortDirection = ProductSalesSortDirection.DESC
)

data class ProductSalesReportSearchMetadata(val snapshotBuiltAt: OffsetDateTime)
