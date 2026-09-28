package me.ezra_home.retail_software_solution.locations.business.sale_payment.search

import jakarta.persistence.Tuple
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentSearchParameters
import me.ezra_home.retail_software_solution.util.queries.SqlQuery
import me.ezra_home.retail_software_solution.util.queries.SqlQueryPerformanceLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SalePaymentSearchFetcher(
  @param:Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_ENTITY_MANAGER_FACTORY)
  private val locationEmf: LocalContainerEntityManagerFactoryBean
) {

  companion object {
    private const val QUERY_TIMEOUT_MS = 2000
  }

  fun search(
    salePaymentSearchParameters: SalePaymentSearchParameters,
    cursor: SalePaymentSearchCursor?,
    requestedSize: Int
  ): List<SalePaymentSearchRawRow> {
    val coercedSize = requestedSize.coerceIn(SalePaymentSearchValidator.MIN_PAGE_SIZE, SalePaymentSearchValidator.MAX_PAGE_SIZE)
    val predicate = SalePaymentSearchQueryBuilder.buildPredicate(salePaymentSearchParameters)
    val sqlQuery = SalePaymentSearchQueryBuilder.buildListQuery(predicate, cursor, coercedSize + 1)
    return execute(sqlQuery).map { SalePaymentSearchRowMapper.fromTuple(it) }
  }

  fun summarize(salePaymentSearchParameters: SalePaymentSearchParameters): List<SalePaymentMethodSummaryRawRow> {
    val predicate = SalePaymentSearchQueryBuilder.buildPredicate(salePaymentSearchParameters)
    val sqlQuery = SalePaymentSearchQueryBuilder.buildSummaryQuery(predicate)
    return execute(sqlQuery).map { SalePaymentSearchRowMapper.summaryFromTuple(it) }
  }

  private fun execute(sqlQuery: SqlQuery): List<Tuple> {
    val startTime = System.currentTimeMillis()
    locationEmf.getObject()!!.createEntityManager().use { em ->
      val query = em.createNativeQuery(sqlQuery.sql, Tuple::class.java)
      sqlQuery.params.forEach { (key, value) -> query.setParameter(key, value) }
      query.setHint("jakarta.persistence.query.timeout", QUERY_TIMEOUT_MS)
      @Suppress("UNCHECKED_CAST")
      val results = query.resultList as List<Tuple>
      SqlQueryPerformanceLogger.logPerformance(startTime, sqlQuery.metadata, results.size)
      return results
    }
  }
}
