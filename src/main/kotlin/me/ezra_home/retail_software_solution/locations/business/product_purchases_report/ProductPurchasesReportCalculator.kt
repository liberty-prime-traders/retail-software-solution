package me.ezra_home.retail_software_solution.locations.business.product_purchases_report

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.product_purchases_report.api.ProductPurchasesDeliveryLineDto
import me.ezra_home.retail_software_solution.locations.business.product_purchases_report.api.ProductPurchasesReportRowDto
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.DisplayFormatters
import me.ezra_home.retail_software_solution.util.quantity.BaseUnitDisplay
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import java.math.RoundingMode

object ProductPurchasesReportCalculator {

  fun toRowDto(
    productPurchasesSummaryRawRow: ProductPurchasesSummaryRawRow,
    locationProductSummaryDto: LocationProductSummaryDto,
    categoryName: String,
    quantityBreakdown: QuantityBreakdown,
    storageUnitsPerBaseUnit: Long
  ): ProductPurchasesReportRowDto {
    val averageCostPerBaseUnit = BaseUnitDisplay.pricePerBaseUnit(
      productPurchasesSummaryRawRow.totalCost,
      productPurchasesSummaryRawRow.storageQuantity,
      storageUnitsPerBaseUnit
    )

    return ProductPurchasesReportRowDto(
      locationProductId = productPurchasesSummaryRawRow.locationProductId,
      referenceNumber = locationProductSummaryDto.referenceNumber,
      productName = locationProductSummaryDto.productName,
      productGroupName = locationProductSummaryDto.productGroupName,
      categoryName = categoryName,
      deliveryLineCount = productPurchasesSummaryRawRow.deliveryLineCount,
      receivedQuantityBreakdown = quantityBreakdown.render(ExactQuantity.ofStorage(productPurchasesSummaryRawRow.storageQuantity)),
      totalCost = Decimals.MoneyScale.round(productPurchasesSummaryRawRow.totalCost),
      averageCostPerBaseUnit = averageCostPerBaseUnit?.let { DisplayFormatters.formatCurrency(it) }
    )
  }

  fun toDeliveryLineDto(
    productPurchasesDeliveryLineRawRow: ProductPurchasesDeliveryLineRawRow,
    supplierName: String,
    quantityBreakdown: QuantityBreakdown,
    storageUnitsPerBaseUnit: Long
  ): ProductPurchasesDeliveryLineDto = ProductPurchasesDeliveryLineDto(
    id = productPurchasesDeliveryLineRawRow.id,
    purchaseReferenceNumber = productPurchasesDeliveryLineRawRow.purchaseReferenceNumber,
    deliveryReferenceNumber = productPurchasesDeliveryLineRawRow.deliveryReferenceNumber,
    deliveredAt = productPurchasesDeliveryLineRawRow.deliveredAt,
    supplierName = supplierName,
    status = productPurchasesDeliveryLineRawRow.status,
    quantityDeliveredText = quantityBreakdown.renderUserProvided(
      productPurchasesDeliveryLineRawRow.userProvidedQuantityDelivered,
      productPurchasesDeliveryLineRawRow.unitId
    ),
    baseQuantity = BaseUnitDisplay.baseQuantity(
      productPurchasesDeliveryLineRawRow.userProvidedQuantityDelivered,
      productPurchasesDeliveryLineRawRow.storageUnitsPerUserProvidedUnit,
      storageUnitsPerBaseUnit,
      RoundingMode.HALF_UP
    ),
    unitCost = productPurchasesDeliveryLineRawRow.unitCost,
    lineTotal = Decimals.MoneyScale.multiply(
      productPurchasesDeliveryLineRawRow.userProvidedQuantityDelivered,
      productPurchasesDeliveryLineRawRow.unitCost
    )
  )
}
