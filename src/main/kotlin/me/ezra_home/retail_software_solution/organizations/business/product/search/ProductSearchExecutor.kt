package me.ezra_home.retail_software_solution.organizations.business.product.search

import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.ParameterNames
import me.ezra_home.retail_software_solution.organizations.model.ProductEntity
import org.springframework.stereotype.Component

@Component
class ProductSearchExecutor(
  private val entityManager: EntityManager,
  private val performanceLogger: ProductSearchPerformanceLogger
) {

  companion object {
    private const val MAX_PAGE_SIZE = 500
    private const val MIN_PAGE_SIZE = 1
  }

  fun executeQuery(sqlQuery: ProductSearchUtilityTypes.SqlQuery, pageSize: Int): List<ProductEntity> {
    val startTime = System.currentTimeMillis()
    val coercedPageSize = pageSize.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)

    val params = sqlQuery.params.toMutableMap()
    params[ParameterNames.PAGE_SIZE] = coercedPageSize

    val query: Query = entityManager.createNativeQuery(sqlQuery.sql, ProductEntity::class.java)
    params.forEach { (key, value) -> query.setParameter(key, value) }

    @Suppress("UNCHECKED_CAST")
    val results = query.resultList as List<ProductEntity>

    performanceLogger.logPerformance(startTime, sqlQuery.metadata, results.size)
    return results
  }
}
