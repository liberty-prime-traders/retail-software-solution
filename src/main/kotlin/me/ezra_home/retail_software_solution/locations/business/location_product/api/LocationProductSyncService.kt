package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductCache
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductDto
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductEntity
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductMapper
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import me.ezra_home.retail_software_solution.util.business.StringUtils
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class LocationProductSyncService(
    private val locationProductRepository: LocationProductRepository,
    private val locationProductCache: LocationProductCache,
    private val locationProductMapper: LocationProductMapper
) {

    @TransactionalOnLocationSchema
    fun syncUpsert(syncDto: LocationProductSyncDto): Boolean {
        val existing = locationProductRepository.findByProductId(syncDto.productId)

        if (existing != null) {
            if (fieldsMatch(existing, syncDto)) return false

            val dto = locationProductMapper.toDomainDto(existing)
            dto.productName = syncDto.productName
            dto.description = syncDto.description
            dto.productGroupName = syncDto.productGroupName ?: ""
            dto.categoryId = syncDto.categoryId
            dto.baseUnitId = syncDto.baseUnitId
            dto.lastSyncedAt = OffsetDateTime.now()
            if (existing.status == ProductStatus.ACTIVE) {
                dto.status = syncDto.status
            }
            locationProductCache.upsertLocationProduct(dto)
            return true
        }

        locationProductCache.upsertLocationProduct(LocationProductDto(
            productId = syncDto.productId,
            productName = syncDto.productName,
            description = syncDto.description,
            productGroupName = syncDto.productGroupName ?: "Unknown",
            categoryId = syncDto.categoryId,
            baseUnitId = syncDto.baseUnitId,
            status = syncDto.status,
            lastSyncedAt = OffsetDateTime.now()
        ))
        return true
    }

    private fun fieldsMatch(existing: LocationProductEntity, syncDto: LocationProductSyncDto): Boolean {
        return StringUtils.isEquivalent(existing.productName, syncDto.productName)
            && StringUtils.isEquivalent(existing.description, syncDto.description)
            && StringUtils.isEquivalent(existing.productGroupName, syncDto.productGroupName)
            && existing.categoryId == syncDto.categoryId
            && existing.baseUnitId == syncDto.baseUnitId
            && existing.status == syncDto.status
    }
}
