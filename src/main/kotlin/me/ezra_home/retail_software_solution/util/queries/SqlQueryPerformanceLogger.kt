package me.ezra_home.retail_software_solution.util.queries

import org.slf4j.LoggerFactory

object SqlQueryPerformanceLogger {

  private const val SLOW_QUERY_THRESHOLD_MS = 1000
  private val logger = LoggerFactory.getLogger(SqlQueryPerformanceLogger::class.java)

  fun logPerformance(startTime: Long, metadata: QueryLogMetadata, resultSize: Int) {
    val duration = System.currentTimeMillis() - startTime
    if (duration > SLOW_QUERY_THRESHOLD_MS) {
      logger.warn("Slow query detected: query={}, duration={}ms, resultSize={}", metadata.queryName, duration, resultSize)
    } else if (logger.isDebugEnabled) {
      logger.debug("Query completed: query={}, duration={}ms, resultCount={}", metadata.queryName, duration, resultSize)
    }
  }
}
