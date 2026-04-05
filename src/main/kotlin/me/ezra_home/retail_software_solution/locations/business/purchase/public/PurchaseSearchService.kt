package me.ezra_home.retail_software_solution.locations.business.purchase.public

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseAssembler
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.queries.SqlQuery
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class PurchaseSearchService(
  private val purchaseRepository: PurchaseRepository,
  private val purchaseAssembler: PurchaseAssembler,
  @param:Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_ENTITY_MANAGER_FACTORY)
  private val locationEmf: LocalContainerEntityManagerFactoryBean
) {

  fun fetchTop(n: Int?): List<PurchaseResponseDto> {
    val recordCount = n ?: 10
    if (recordCount > 1000) throw RtsGenericException("Limit exceeds maximum of 1000")
    val sort = Sort.by(Sort.Direction.DESC, "createdOn")
    return purchaseAssembler.buildResponses(purchaseRepository.findTopN(PageRequest.of(0, recordCount, sort)))
  }

  private fun execute(sqlQuery: SqlQuery): List<PurchaseEntity> {
    locationEmf.getObject()!!.createEntityManager().use { em ->
      val query = em.createNativeQuery(sqlQuery.sql, PurchaseEntity::class.java)
      sqlQuery.params.forEach { (key, value) -> query.setParameter(key, value) }
      @Suppress("UNCHECKED_CAST")
      return query.resultList as List<PurchaseEntity>
    }
  }

}
