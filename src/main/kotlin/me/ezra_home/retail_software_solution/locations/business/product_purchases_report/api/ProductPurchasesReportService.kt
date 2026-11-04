package me.ezra_home.retail_software_solution.locations.business.product_purchases_report.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.product_purchases_report.ProductPurchasesReportCalculator
import me.ezra_home.retail_software_solution.locations.business.product_purchases_report.ProductPurchasesReportFetcher
import me.ezra_home.retail_software_solution.locations.business.product_purchases_report.ProductPurchasesReportValidator
import me.ezra_home.retail_software_solution.locations.business.stock.api.QuantityBreakdownFactory
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryService
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class ProductPurchasesReportService(
  private val productPurchasesReportFetcher: ProductPurchasesReportFetcher,
  private val locationProductDataFetcher: LocationProductDataFetcher,
  private val unitConversionGraphFacade: UnitConversionGraphFacade,
  private val quantityBreakdownFactory: QuantityBreakdownFactory,
  private val contactService: ContactService,
  private val productCategoryService: ProductCategoryService
) {

  fun summarize(productPurchasesReportParameters: ProductPurchasesReportParameters): List<ProductPurchasesReportRowDto> {
    val deliveredRange = ProductPurchasesReportValidator.resolveDeliveredRange(
      productPurchasesReportParameters.deliveredFrom,
      productPurchasesReportParameters.deliveredBefore
    )
    val rawRows = productPurchasesReportFetcher.fetchSummaries(productPurchasesReportParameters, deliveredRange)
    if (rawRows.isEmpty()) return emptyList()

    val summariesById = locationProductDataFetcher.findSummaryByIds(rawRows.map { it.locationProductId })
    val categoryNamesById = productCategoryService.getCategoryNamesById()
    val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
    val quantityBreakdownsByBaseUnitId = quantityBreakdownFactory.forBaseUnits(summariesById.values.map { it.baseUnitId })

    return rawRows.map { productPurchasesSummaryRawRow ->
      val summary = summariesById.getValue(productPurchasesSummaryRawRow.locationProductId)
      ProductPurchasesReportCalculator.toRowDto(
        productPurchasesSummaryRawRow = productPurchasesSummaryRawRow,
        locationProductSummaryDto = summary,
        categoryName = categoryNamesById.getValue(productPurchasesSummaryRawRow.categoryId),
        quantityBreakdown = quantityBreakdownsByBaseUnitId.getValue(summary.baseUnitId),
        storageUnitsPerBaseUnit = unitConversionGraph.getSizeInRoot(summary.baseUnitId)
      )
    }.sortedBy { it.productName.lowercase() }
  }

  fun searchDeliveryLines(
    locationProductId: UUID,
    pageRequest: PageRequest<ProductPurchasesDeliveryLinesParameters, String>
  ): PageResponse<ProductPurchasesDeliveryLineDto, String> {
    val productPurchasesDeliveryLinesParameters = pageRequest.parameters
    val deliveredRange = ProductPurchasesReportValidator.resolveDeliveredRange(
      productPurchasesDeliveryLinesParameters.deliveredFrom,
      productPurchasesDeliveryLinesParameters.deliveredBefore
    )
    ProductPurchasesReportValidator.guardValidPageSize(pageRequest.requestedSize)
    val cursor = KeysetSearchCursor.decode(pageRequest.previousCursor)

    val summary = locationProductDataFetcher.findSummaryByIds(listOf(locationProductId))[locationProductId]
      ?: throw RtsGenericException("Location product not found: $locationProductId")
    val quantityBreakdown = quantityBreakdownFactory.forBaseUnits(listOf(summary.baseUnitId)).getValue(summary.baseUnitId)
    val storageUnitsPerBaseUnit = unitConversionGraphFacade.getOrLoad().getSizeInRoot(summary.baseUnitId)

    val rawRows = productPurchasesReportFetcher.fetchDeliveryLines(
      locationProductId,
      productPurchasesDeliveryLinesParameters,
      deliveredRange,
      cursor,
      pageRequest.requestedSize
    )
    val hasMore = rawRows.size > pageRequest.requestedSize
    val pageRows = if (hasMore) rawRows.take(pageRequest.requestedSize) else rawRows

    val supplierNamesById = contactService.getAllContactDtos().associateBy({ it.id }, { it.identity.displayName })
    val contents = pageRows.map { productPurchasesDeliveryLineRawRow ->
      ProductPurchasesReportCalculator.toDeliveryLineDto(
        productPurchasesDeliveryLineRawRow = productPurchasesDeliveryLineRawRow,
        supplierName = supplierNamesById.getValue(productPurchasesDeliveryLineRawRow.supplierId),
        quantityBreakdown = quantityBreakdown,
        storageUnitsPerBaseUnit = storageUnitsPerBaseUnit
      )
    }

    val currentCursor = pageRows.lastOrNull()
      ?.let { KeysetSearchCursor(it.deliveredAt, it.id).encode() }
      ?: pageRequest.previousCursor

    return PageResponse(currentCursor = currentCursor, hasMore = hasMore, contents = contents)
  }
}
