package me.ezra_home.retail_software_solution.locations.business.purchase.search

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseSearchParameters
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Component

@Component
@TransactionalOnLocationSchema(readOnly = true)
class PurchaseSearchFetcher(
  private val purchaseSearchListExecutor: PurchaseSearchListExecutor,
  private val purchaseSearchSummaryExecutor: PurchaseSearchSummaryExecutor
) {

  fun search(
    purchaseSearchParameters: PurchaseSearchParameters,
    cursor: KeysetSearchCursor?,
    requestedSize: Int
  ): List<PurchaseEntity> {
    val predicate = PurchaseSearchQueryBuilder.buildPredicate(purchaseSearchParameters)
    val sqlQuery = PurchaseSearchQueryBuilder.buildListQuery(predicate, cursor)
    return purchaseSearchListExecutor.execute(sqlQuery, requestedSize + 1, setTimeout = true)
  }

  fun summarizeByPaymentStatus(purchaseSearchParameters: PurchaseSearchParameters): List<PurchasePaymentStatusSummaryRawRow> {
    val predicate = PurchaseSearchQueryBuilder.buildPredicate(purchaseSearchParameters)
    val sqlQuery = PurchaseSearchQueryBuilder.buildPaymentStatusSummaryQuery(predicate)
    return purchaseSearchSummaryExecutor.executeUnpaged(sqlQuery, setTimeout = true)
      .map { PurchaseSearchRowMapper.paymentStatusSummaryFromTuple(it) }
  }

  fun summarizeBySupplier(purchaseSearchParameters: PurchaseSearchParameters): List<PurchaseSupplierSummaryRawRow> {
    val predicate = PurchaseSearchQueryBuilder.buildPredicate(purchaseSearchParameters)
    val sqlQuery = PurchaseSearchQueryBuilder.buildSupplierSummaryQuery(predicate)
    return purchaseSearchSummaryExecutor.executeUnpaged(sqlQuery, setTimeout = true)
      .map { PurchaseSearchRowMapper.supplierSummaryFromTuple(it) }
  }
}
