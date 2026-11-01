package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSortDirection
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSortField
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.StringUtils
import org.springframework.stereotype.Component
import java.time.LocalDate

// The only transactional step of the search: a request served from the snapshot cache never opens a connection.
@Component
@TransactionalOnLocationSchema(readOnly = true)
class ProductSalesReportSnapshotBuilder(
  private val productSalesReportFetcher: ProductSalesReportFetcher,
  private val productSalesDayBucketCache: ProductSalesDayBucketCache,
  private val productSalesSummaryAssembler: ProductSalesSummaryAssembler
) {

  fun build(window: ProductSalesReportWindow): ProductSalesReportSnapshot {
    val dayBuckets = productSalesDayBucketCache.getOrLoad(window.days) { missingDays -> loadDayBuckets(missingDays) }
    val rawRows = ProductSalesReportCalculator.sumByProduct(dayBuckets.flatMap { it.rawRowsByLocationProductId.values })
    val needsAttentionComparator = ProductSalesReportOrdering.comparator(ProductSalesSortDirection.DESC)
    val rankedRows = productSalesSummaryAssembler.assemble(rawRows.map { it.locationProductId }, rawRows)
      .map { summary ->
        ProductSalesReportKeyedRow(
          ProductSalesReportSnapshotRow(
            summary = summary,
            comparableProductName = StringUtils.normalizeForComparison(summary.productName),
            comparableProductGroupName = StringUtils.normalizeForComparison(summary.productGroupName)
          ),
          ProductSalesReportOrdering.keyOf(summary, ProductSalesSortField.NEEDS_ATTENTION)
        )
      }
      .sortedWith { first, second -> needsAttentionComparator.compare(first.key, second.key) }
      .mapIndexed { index, keyedRow -> keyedRow.row.copy(summary = keyedRow.row.summary.copy(rank = index + 1)) }
    return ProductSalesReportSnapshot(dayBuckets.minOf { it.builtAt }, rankedRows)
  }

  // One query per run of consecutive missing days, so cached days between two gaps are never aggregated again.
  private fun loadDayBuckets(missingDays: List<LocalDate>): Map<LocalDate, ProductSalesDayBucket> {
    val builtAt = DateTimes.Offset.Now.organization()
    val rawRowsByDay = consecutiveRuns(missingDays)
      .flatMap { run -> productSalesReportFetcher.fetchDailySummaries(ProductSalesReportWindow(run.first(), run.last().plusDays(1))) }
      .groupBy({ it.soldOn }, { it.summary })
    return missingDays.associateWith { day ->
      ProductSalesDayBucket(builtAt, rawRowsByDay[day].orEmpty().associateBy { it.locationProductId })
    }
  }

  private fun consecutiveRuns(ascendingDays: List<LocalDate>): List<List<LocalDate>> {
    val runs = mutableListOf<MutableList<LocalDate>>()
    ascendingDays.forEach { day ->
      val currentRun = runs.lastOrNull()
      if (currentRun != null && currentRun.last().plusDays(1) == day) currentRun.add(day) else runs.add(mutableListOf(day))
    }
    return runs
  }
}
