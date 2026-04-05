package me.ezra_home.retail_software_solution.cross_tier.product.search.common

import jakarta.persistence.Query
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductEntity
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductEntity
import me.ezra_home.retail_software_solution.util.queries.QueryMetadata
import me.ezra_home.retail_software_solution.util.queries.SqlQuery
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component

@Component
class ProductSearchExecutor(
  @param:Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_ENTITY_MANAGER_FACTORY)
  private val organizationEmf: LocalContainerEntityManagerFactoryBean,
  @param:Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_ENTITY_MANAGER_FACTORY)
  private val locationEmf: LocalContainerEntityManagerFactoryBean
) {

  companion object {
    private const val MAX_PAGE_SIZE = 500
    private const val MIN_PAGE_SIZE = 1
  }

  fun executeOrgQuery(
    sqlQuery: SqlQuery,
    pageSize: Int,
    setTimeout: Boolean
  ): List<OrganizationProductEntity> {
    return executeQuery(organizationEmf, sqlQuery.sql, sqlQuery.params, sqlQuery.metadata, pageSize, setTimeout, OrganizationProductEntity::class.java)
  }

  fun executeLocationQuery(
    sqlQuery: SqlQuery,
    pageSize: Int,
    setTimeout: Boolean
  ): List<LocationProductEntity> {
    return executeQuery(locationEmf, sqlQuery.sql, sqlQuery.params, sqlQuery.metadata, pageSize, setTimeout, LocationProductEntity::class.java)
  }

  private fun <T> executeQuery(
    emf: LocalContainerEntityManagerFactoryBean,
    sql: String,
    params: Map<String, Any>,
    metadata: QueryMetadata,
    pageSize: Int,
    setTimeout: Boolean,
    entityClass: Class<T>
  ): List<T> {
    val startTime = System.currentTimeMillis()
    val coercedPageSize = pageSize.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)

    val queryParams = params.toMutableMap()
    queryParams[ParameterNames.PAGE_SIZE] = coercedPageSize

    emf.getObject()!!.createEntityManager().use { entityManager ->
      val query: Query = entityManager.createNativeQuery(sql, entityClass)
      queryParams.forEach { (key, value) -> query.setParameter(key, value) }
      if (setTimeout) {
        query.setHint("jakarta.persistence.query.timeout", 2000)
      }
      @Suppress("UNCHECKED_CAST")
      val results = query.resultList as List<T>

      ProductSearchPerformanceLogger.logPerformance(startTime, metadata, results.size)

      return results
    }
  }
}
