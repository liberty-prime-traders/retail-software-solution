package me.ezra_home.retail_software_solution.locations.business.product_sales_report.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportCalculator
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportFetcher
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportValidator
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesSummaryAssembler
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonService
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.QuantityBreakdownFactory
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class ProductSalesReportService(
  private val productSalesReportFetcher: ProductSalesReportFetcher,
  private val productSalesSummaryAssembler: ProductSalesSummaryAssembler,
  private val locationProductDataFetcher: LocationProductDataFetcher,
  private val unitConversionGraphFacade: UnitConversionGraphFacade,
  private val quantityBreakdownFactory: QuantityBreakdownFactory,
  private val adjustmentReasonService: AdjustmentReasonService
) {

  fun searchLines(
    locationProductId: UUID,
    pageRequest: PageRequest<ProductSalesLinesParameters, String>
  ): PageResponse<ProductSalesLineDto, String> {
    val productSalesLinesParameters = pageRequest.parameters
    val dateRange = ProductSalesReportValidator.resolveDateRange(
      productSalesLinesParameters.from,
      productSalesLinesParameters.before
    )
    ProductSalesReportValidator.guardValidPageSize(pageRequest.requestedSize)
    val cursor = KeysetSearchCursor.decode(pageRequest.previousCursor)

    val baseUnitId = requireBaseUnitIdsByLocationProductId(listOf(locationProductId)).getValue(locationProductId)

    val rawRows = productSalesReportFetcher.fetchLines(locationProductId, dateRange, cursor, pageRequest.requestedSize)
    val hasMore = rawRows.size > pageRequest.requestedSize
    val pageRows = if (hasMore) rawRows.take(pageRequest.requestedSize) else rawRows

    val adjustmentRowsBySaleLineId = productSalesReportFetcher.fetchLineAdjustments(pageRows.map { it.saleLineId })
      .groupBy { it.saleLineId }
    val reasonNamesById = adjustmentReasonService.getReasonNamesById()
    val quantityBreakdown = quantityBreakdownFactory.forBaseUnits(listOf(baseUnitId)).getValue(baseUnitId)
    val storageUnitsPerBaseUnit = unitConversionGraphFacade.getOrLoad().getSizeInRoot(baseUnitId)

    val contents = pageRows.map { rawRow ->
      val adjustments = adjustmentRowsBySaleLineId[rawRow.saleLineId].orEmpty().map {
        ProductSalesLineAdjustmentDto(
          reasonName = reasonNamesById.getValue(it.adjustmentReasonId),
          direction = it.direction,
          amount = ProductSalesReportCalculator.signedAmount(it.direction, it.calculatedAmount)
        )
      }
      ProductSalesReportCalculator.toLineDto(
        rawRow = rawRow,
        adjustments = adjustments,
        quantityEntered = quantityBreakdown.renderUserProvided(rawRow.userProvidedQuantity, rawRow.unitId),
        storageUnitsPerBaseUnit = storageUnitsPerBaseUnit
      )
    }

    val currentCursor = pageRows.lastOrNull()
      ?.let { KeysetSearchCursor(it.dateSold, it.saleLineId).encode() }
      ?: pageRequest.previousCursor

    return PageResponse(currentCursor = currentCursor, hasMore = hasMore, contents = contents)
  }

  fun summarize(productSalesSummaryParameters: ProductSalesSummaryParameters): List<ProductSalesSummaryDto> {
    val locationProductIds = productSalesSummaryParameters.locationProductIds
    ProductSalesReportValidator.guardValidSummaryProductIds(locationProductIds)
    val dateRange = ProductSalesReportValidator.resolveDateRange(
      productSalesSummaryParameters.from,
      productSalesSummaryParameters.before
    )
    val rawRows = productSalesReportFetcher.fetchSummaries(locationProductIds, dateRange)
    return productSalesSummaryAssembler.assemble(locationProductIds, rawRows)
  }

  private fun requireBaseUnitIdsByLocationProductId(locationProductIds: List<UUID>): Map<UUID, UUID> {
    val baseUnitIdsByLocationProductId = locationProductDataFetcher.getBaseUnitIds(locationProductIds)
    val missingLocationProductIds = locationProductIds.filter { it !in baseUnitIdsByLocationProductId }
    if (missingLocationProductIds.isNotEmpty()) {
      throw RtsGenericException("Location products not found: ${missingLocationProductIds.joinToString()}")
    }
    return baseUnitIdsByLocationProductId
  }
}
