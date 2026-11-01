package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.QuantityBreakdownFactory
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSummaryDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductSalesSummaryAssembler(
  private val locationProductDataFetcher: LocationProductDataFetcher,
  private val unitConversionGraphFacade: UnitConversionGraphFacade,
  private val unitValueFetcher: UnitValueFetcher,
  private val quantityBreakdownFactory: QuantityBreakdownFactory
) {

  fun assemble(locationProductIds: List<UUID>, rawRows: List<ProductSalesSummaryRawRow>): List<ProductSalesSummaryDto> {
    val summariesById = locationProductDataFetcher.findSummaryByIds(locationProductIds)
    val missingLocationProductIds = locationProductIds.filter { it !in summariesById }
    if (missingLocationProductIds.isNotEmpty()) {
      throw RtsGenericException("Location products not found: ${missingLocationProductIds.joinToString()}")
    }

    val rawRowsByLocationProductId = rawRows.associateBy { it.locationProductId }
    val unitNamesById = unitValueFetcher.getUnitNamesById()
    val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
    val quantityBreakdownsByBaseUnitId = quantityBreakdownFactory.forBaseUnits(summariesById.values.map { it.baseUnitId })

    return locationProductIds.map { locationProductId ->
      val summary = summariesById.getValue(locationProductId)
      ProductSalesReportCalculator.toSummaryDto(
        rawRow = rawRowsByLocationProductId[locationProductId]
          ?: ProductSalesReportCalculator.emptySummaryRawRow(locationProductId),
        locationProductSummaryDto = summary,
        quantityBreakdown = quantityBreakdownsByBaseUnitId.getValue(summary.baseUnitId),
        baseUnitName = unitNamesById.getValue(summary.baseUnitId),
        storageUnitsPerBaseUnit = unitConversionGraph.getSizeInRoot(summary.baseUnitId)
      )
    }
  }
}
