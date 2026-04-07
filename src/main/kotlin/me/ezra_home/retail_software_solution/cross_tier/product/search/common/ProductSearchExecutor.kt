package me.ezra_home.retail_software_solution.cross_tier.product.search.common

import me.ezra_home.retail_software_solution.util.queries.SqlQuery
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean

abstract class ProductSearchExecutor<T, D>(
  private val emf: LocalContainerEntityManagerFactoryBean,
  private val entityClass: Class<T>
) {

  companion object {
    private const val MAX_PAGE_SIZE = 500
    private const val MIN_PAGE_SIZE = 1
  }

  protected abstract fun map(entities: List<T>): List<D>

  fun execute(sqlQuery: SqlQuery, pageSize: Int, setTimeout: Boolean): List<D> {
    val startTime = System.currentTimeMillis()
    val coercedPageSize = pageSize.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)

    val queryParams = sqlQuery.params.toMutableMap()
    queryParams[ParameterNames.PAGE_SIZE] = coercedPageSize

    emf.getObject()!!.createEntityManager().use { entityManager ->
      val query = entityManager.createNativeQuery(sqlQuery.sql, entityClass)
      queryParams.forEach { (key, value) -> query.setParameter(key, value) }
      if (setTimeout) {
        query.setHint("jakarta.persistence.query.timeout", 2000)
      }
      @Suppress("UNCHECKED_CAST")
      val results = query.resultList as List<T>

      ProductSearchPerformanceLogger.logPerformance(startTime, sqlQuery.metadata, results.size)

      return map(results)
    }
  }
}
