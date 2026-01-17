package me.ezra_home.retail_software_solution.organizations.business.product.search

import jakarta.persistence.Query
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.ParameterNames
import me.ezra_home.retail_software_solution.organizations.model.ProductEntity
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component

@Component
class ProductSearchExecutor(
  @param:Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_ENTITY_MANAGER_FACTORY)
  private val organizationEmf: LocalContainerEntityManagerFactoryBean
) {

  companion object {
    private const val MAX_PAGE_SIZE = 500
    private const val MIN_PAGE_SIZE = 1
  }

  fun executeQuery(
    sqlQuery: ProductSearchUtilityTypes.SqlQuery,
    pageSize: Int,
    setTimeout: Boolean,
  ): List<ProductEntity> {

    val startTime = System.currentTimeMillis()
    val coercedPageSize = pageSize.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)

    val params = sqlQuery.params.toMutableMap()
    params[ParameterNames.PAGE_SIZE] = coercedPageSize

    val entityManager = organizationEmf.getObject()!!.createEntityManager()
    val query: Query = entityManager.createNativeQuery(sqlQuery.sql, ProductEntity::class.java)
    params.forEach { (key, value) -> query.setParameter(key, value) }
    if (setTimeout) {
      query.setHint("jakarta.persistence.query.timeout", 2000)
    }

    @Suppress("UNCHECKED_CAST")
    val results = query.resultList as List<ProductEntity>

    ProductSearchPerformanceLogger.logPerformance(startTime, sqlQuery.metadata, results.size)
    return results
  }
}
