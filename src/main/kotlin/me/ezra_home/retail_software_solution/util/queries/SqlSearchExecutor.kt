package me.ezra_home.retail_software_solution.util.queries

import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean

abstract class SqlSearchExecutor<T, D>(
  private val emf: LocalContainerEntityManagerFactoryBean,
  private val resultClass: Class<T>
) {

  companion object {
    // Callers pass requestedSize + 1 (one extra row to detect hasMore), so this must exceed the
    // largest MAX_PAGE_SIZE any validator allows (currently 500) — otherwise the extra row gets
    // coerced away right at the largest page size and hasMore false-negatives on the last page.
    private const val MAX_PAGE_SIZE = 501
    private const val MIN_PAGE_SIZE = 1
    private const val QUERY_TIMEOUT_MS = 2000
  }

  protected abstract fun map(entities: List<T>): List<D>

  fun execute(sqlQuery: SqlQuery, pageSize: Int, setTimeout: Boolean): List<D> {
    val coercedPageSize = pageSize.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)
    val queryParams = sqlQuery.params.toMutableMap()
    queryParams[QueryParameterNames.PAGE_SIZE] = coercedPageSize
    return runQuery(sqlQuery.copy(params = queryParams), setTimeout)
  }

  fun executeUnpaged(sqlQuery: SqlQuery, setTimeout: Boolean): List<D> = runQuery(sqlQuery, setTimeout)

  private fun runQuery(sqlQuery: SqlQuery, setTimeout: Boolean): List<D> {
    val startTime = System.currentTimeMillis()

    emf.getObject()!!.createEntityManager().use { entityManager ->
      val query = entityManager.createNativeQuery(sqlQuery.sql, resultClass)
      sqlQuery.params.forEach { (key, value) -> query.setParameter(key, value) }
      if (setTimeout) {
        query.setHint("jakarta.persistence.query.timeout", QUERY_TIMEOUT_MS)
      }
      @Suppress("UNCHECKED_CAST")
      val results = query.resultList as List<T>

      SqlQueryPerformanceLogger.logPerformance(startTime, sqlQuery.metadata, results.size)

      return map(results)
    }
  }
}
