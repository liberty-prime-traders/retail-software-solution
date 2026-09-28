package me.ezra_home.retail_software_solution.util.queries

import org.slf4j.LoggerFactory

object SqlQueryPerformanceLogger {

  private const val SLOW_QUERY_THRESHOLD_MS = 1000
  private val logger = LoggerFactory.getLogger(SqlQueryPerformanceLogger::class.java)

  fun logPerformance(startTime: Long, metadata: QueryLogMetadata, resultSize: Int) {
    val duration = System.currentTimeMillis() - startTime
    if (duration > SLOW_QUERY_THRESHOLD_MS) {
      logSlowQuery(metadata, duration, resultSize)
    } else if (logger.isDebugEnabled) {
      logger.debug("Query completed: query={}, duration={}ms, resultCount={}", metadata.queryName, duration, resultSize)
    }
  }

  private fun logSlowQuery(metadata: QueryLogMetadata, duration: Long, resultSize: Int) {
    if (metadata !is ProductQueryMetadata) {
      logger.warn("Slow query detected: query={}, duration={}ms, resultSize={}", metadata.queryName, duration, resultSize)
      return
    }

    val message = buildString {
      append("Slow query detected: query={}, duration={}ms, categoryIdsCount={}, ")
      append("statusListCount={}, hasTextSearch={}")
      if (metadata.tagIdsCount != null) append(", tagIdsCount={}")
      if (metadata.hasReferenceNumberSearch != null) append(", hasReferenceNumberSearch={}")
      if (metadata.hasTagFilter != null) append(", hasTagFilter={}")
      append(", resultSize={}")
    }

    val args = mutableListOf<Any>(
      metadata.queryName,
      duration,
      metadata.categoryIdsCount,
      metadata.statusListCount,
      metadata.hasTextSearch
    )
    if (metadata.tagIdsCount != null) args.add(metadata.tagIdsCount)
    if (metadata.hasReferenceNumberSearch != null) args.add(metadata.hasReferenceNumberSearch)
    if (metadata.hasTagFilter != null) args.add(metadata.hasTagFilter)
    args.add(resultSize)

    logger.warn(message, *args.toTypedArray())
  }
}
