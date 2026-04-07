package me.ezra_home.retail_software_solution.locations.business.catalog_sync.sync_services.product

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncCursor
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.sync_services.SyncService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSyncService
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductFetcher
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProductSyncService(
  private val productRevisionFetcher: ProductRevisionFetcher,
  private val locationProductSyncService: LocationProductSyncService,
  private val organizationProductFetcher: OrganizationProductFetcher
) : SyncService<ProductSyncData> {

  override val tableName = TableName.PRODUCT

  @TransactionalOnOrganizationSchema(readOnly = true)
  override fun countAllRecords(): Int {
    return organizationProductFetcher.countAllProducts().toInt()
  }

  @TransactionalOnOrganizationSchema(readOnly = true)
  override fun fetchBatch(cursor: SyncCursor?, batchSize: Int): List<ProductSyncData> {
    return when (cursor) {
      is SyncCursor.Reference -> productRevisionFetcher.fetchBatchForFull(cursor.value, batchSize)
      is SyncCursor.Revision -> productRevisionFetcher.fetchBatchForIncremental(cursor.value, batchSize)
      null -> productRevisionFetcher.fetchBatchForFull(null, batchSize)
    }
  }

  override fun extractCursor(record: ProductSyncData): SyncCursor {
    return if (record.revision != null) {
      SyncCursor.Revision(record.revision)
    } else {
      SyncCursor.Reference(record.referenceNumber)
    }
  }

  @TransactionalOnLocationSchema
  override fun createLocationRecord(record: ProductSyncData): Boolean {
    return locationProductSyncService.syncUpsert(LocationProductMapper.toSyncDto(record))
  }

  @TransactionalOnOrganizationSchema(readOnly = true)
  override fun syncSingle(entityId: UUID) {
    val productSyncData = productRevisionFetcher.fetchByProductId(entityId) ?: return
    createLocationRecord(productSyncData)
  }
}
