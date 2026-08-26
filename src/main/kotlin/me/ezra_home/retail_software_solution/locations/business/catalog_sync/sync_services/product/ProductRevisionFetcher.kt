package me.ezra_home.retail_software_solution.locations.business.catalog_sync.sync_services.product

import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Tuple
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.sync_services.SyncQueryConstants
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductRevisionFetcher(
  @param:Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_ENTITY_MANAGER_FACTORY)
  private val organizationEmf: EntityManagerFactory
) {

  fun fetchBatchForFull(afterReferenceNumber: String?, batchSize: Int): List<ProductSyncData> {
    organizationEmf.createEntityManager().use { em ->
      val sql = ProductQueryBuilder.buildFetchQueryForFull(afterReferenceNumber)
      val query = em.createNativeQuery(sql, Tuple::class.java)
      if (afterReferenceNumber != null) {
        query.setParameter(SyncQueryConstants.CursorParameters.AFTER_REFERENCE_NUMBER, afterReferenceNumber)
      }
      query.maxResults = batchSize

      @Suppress("UNCHECKED_CAST")
      val results = query.resultList as List<Tuple>
      return results.map { ProductSyncDataMapper.fromTuple(it) }
    }
  }

  fun fetchBatchForIncremental(afterRevision: Long, batchSize: Int): List<ProductSyncData> {
    organizationEmf.createEntityManager().use { em ->
      val sql = ProductQueryBuilder.buildFetchQueryForIncremental()
      val query = em.createNativeQuery(sql, Tuple::class.java)
      query.setParameter(SyncQueryConstants.CursorParameters.AFTER_REVISION, afterRevision)
      query.maxResults = batchSize

      @Suppress("UNCHECKED_CAST")
      val results = query.resultList as List<Tuple>
      return results.map { ProductSyncDataMapper.fromTuple(it) }
    }
  }

  fun fetchByOrgProductId(orgProductId: UUID): ProductSyncData? {
    organizationEmf.createEntityManager().use { em ->
      val sql = ProductQueryBuilder.buildFetchQueryByOrgProductId()
      val query = em.createNativeQuery(sql, Tuple::class.java)
      query.setParameter(ProductQueryConstants.Parameters.ORG_PRODUCT_ID, orgProductId)

      @Suppress("UNCHECKED_CAST")
      val results = query.resultList as List<Tuple>
      return results.firstOrNull()?.let { ProductSyncDataMapper.fromTuple(it) }
    }
  }
}
