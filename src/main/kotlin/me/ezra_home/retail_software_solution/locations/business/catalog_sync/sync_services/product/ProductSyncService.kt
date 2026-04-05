package me.ezra_home.retail_software_solution.locations.business.catalog_sync.sync_services.product

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncCursor
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.sync_services.SyncService
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductCache
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductEntity
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductRepository
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.organizations.business.product.public.ProductStatus
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ProductSyncService(
  private val productRevisionFetcher: ProductRevisionFetcher,
  private val locationProductRepository: LocationProductRepository,
  private val locationProductCache: LocationProductCache,
  private val organizationProductRepository: OrganizationProductRepository
) : SyncService<ProductSyncData> {

  override val tableName = TableName.PRODUCT

  @TransactionalOnOrganizationSchema(readOnly = true)
  override fun countAllRecords(): Int {
    return organizationProductRepository.count().toInt()
  }

  @TransactionalOnOrganizationSchema(readOnly = true)
  override fun fetchBatch(cursor: SyncCursor?, batchSize: Int): List<ProductSyncData> {
    return when (cursor) {
      is SyncCursor.Reference -> {
        productRevisionFetcher.fetchBatchForFull(cursor.value, batchSize)
      }
      is SyncCursor.Revision -> {
        productRevisionFetcher.fetchBatchForIncremental(cursor.value, batchSize)
      }
      null -> {
        productRevisionFetcher.fetchBatchForFull(null, batchSize)
      }
    }
  }

  override fun extractCursor(record: ProductSyncData): SyncCursor {
    return if (record.revision != null) {
      SyncCursor.Revision(record.revision)
    } else {
      val referenceNumber = record.referenceNumber
      SyncCursor.Reference(referenceNumber)
    }
  }

  @TransactionalOnLocationSchema
  override fun createLocationRecord(record: ProductSyncData): Boolean {
    val existing = locationProductRepository.findByProductId(record.productId)

    if (existing != null) {
      val needsUpdate = !fieldsMatch(existing, record)

      if (!needsUpdate) {
        return false
      }

      existing.productName = record.productName
      existing.description = record.description
      existing.productGroupName = record.productGroupName ?: ""
      existing.categoryId = record.categoryId
      existing.baseUnitId = record.baseUnitId
      existing.lastSyncedAt = OffsetDateTime.now()
      if (existing.status == ProductStatus.ACTIVE) {
        existing.status = record.status
      }

      locationProductCache.upsertLocationProduct(existing)
      return true
    }

    val locationProduct = LocationProductMapper.toLocationProduct(record)
    locationProductCache.upsertLocationProduct(locationProduct)
    return true
  }

  private fun fieldsMatch(existing: LocationProductEntity, syncData: ProductSyncData): Boolean {
    return StringUtils.isEquivalent(existing.productName, syncData.productName)
      && StringUtils.isEquivalent(existing.description, syncData.description)
      && StringUtils.isEquivalent(existing.productGroupName, syncData.productGroupName)
      && existing.categoryId == syncData.categoryId
      && existing.baseUnitId == syncData.baseUnitId
      && existing.status == syncData.status
  }

  @TransactionalOnOrganizationSchema(readOnly = true)
  override fun syncSingle(entityId: UUID) {
    val productSyncData = productRevisionFetcher.fetchByProductId(entityId) ?: return
    createLocationRecord(productSyncData)
  }
}
