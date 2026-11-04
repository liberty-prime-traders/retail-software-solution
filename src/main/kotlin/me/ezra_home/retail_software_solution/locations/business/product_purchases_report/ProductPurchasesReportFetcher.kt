package me.ezra_home.retail_software_solution.locations.business.product_purchases_report

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.product_purchases_report.api.ProductPurchasesDeliveryLinesParameters
import me.ezra_home.retail_software_solution.locations.business.product_purchases_report.api.ProductPurchasesReportParameters
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class ProductPurchasesReportFetcher(
  private val productPurchasesReportExecutor: ProductPurchasesReportExecutor
) {

  fun fetchSummaries(
    productPurchasesReportParameters: ProductPurchasesReportParameters,
    deliveredRange: ProductPurchasesDeliveredRange
  ): List<ProductPurchasesSummaryRawRow> {
    val predicate = ProductPurchasesReportQueryBuilder.buildPredicate(productPurchasesReportParameters, deliveredRange)
    val sqlQuery = ProductPurchasesReportQueryBuilder.buildSummaryQuery(predicate)
    return productPurchasesReportExecutor.executeUnpaged(sqlQuery, setTimeout = true)
      .map { ProductPurchasesReportRowMapper.summaryFromTuple(it) }
  }

  fun fetchDeliveryLines(
    locationProductId: UUID,
    productPurchasesDeliveryLinesParameters: ProductPurchasesDeliveryLinesParameters,
    deliveredRange: ProductPurchasesDeliveredRange,
    cursor: KeysetSearchCursor?,
    requestedSize: Int
  ): List<ProductPurchasesDeliveryLineRawRow> {
    val sqlQuery = ProductPurchasesReportQueryBuilder.buildDeliveryLinesQuery(
      locationProductId,
      productPurchasesDeliveryLinesParameters,
      deliveredRange,
      cursor
    )
    return productPurchasesReportExecutor.execute(sqlQuery, requestedSize + 1, setTimeout = true)
      .map { ProductPurchasesReportRowMapper.deliveryLineFromTuple(it) }
  }
}
