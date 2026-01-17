package me.ezra_home.retail_software_solution.organizations.business.product.search

import org.slf4j.LoggerFactory

object ProductSearchPerformanceLogger {

  private const val SLOW_QUERY_THRESHOLD_MS = 1000
  private val logger = LoggerFactory.getLogger(ProductSearchPerformanceLogger::class.java)

  fun logPerformance(startTime: Long, metadata: ProductSearchUtilityTypes.QueryMetadata, resultSize: Int) {
    val duration = System.currentTimeMillis() - startTime
    if (duration > SLOW_QUERY_THRESHOLD_MS) {
      logger.warn(
        "Slow query detected: query={}, duration={}ms, categoryIdsCount={}, " +
        "tagIdsCount={}, statusListCount={}, hasTextSearch={}, hasReferenceNumberSearch={}, hasTagFilter={}, resultSize={}",
        metadata.queryName,
        duration,
        metadata.categoryIdsCount,
        metadata.tagIdsCount,
        metadata.statusListCount,
        metadata.hasTextSearch,
        metadata.hasReferenceNumberSearch,
        metadata.hasTagFilter,
        resultSize
      )
    } else if (logger.isDebugEnabled) {
      logger.debug("Product search completed: query={}, duration={}ms, resultCount={}", metadata.queryName, duration, resultSize)
    }
  }
}
