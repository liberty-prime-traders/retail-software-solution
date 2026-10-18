package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductCache
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductEntity
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductInsertDto
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductMapper
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
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
        val existing = locationProductRepository.findByOrgProductId(syncDto.orgProductId)

        if (existing != null) {
            // Stock is counted in the storage unit of the base unit's group, so a different base unit would re-meaning every stored quantity.
            if (existing.baseUnitId != syncDto.baseUnitId) throw RtsGenericException(BASE_UNIT_CANNOT_CHANGE)
            if (fieldsMatch(existing, syncDto)) return false

            val dto = locationProductMapper.toDomainDto(existing)
            val updatedStatus = if (existing.status == ProductStatus.ACTIVE) syncDto.status else dto.status
            locationProductCache.save(dto.copy(
                productName = syncDto.productName,
                description = syncDto.description,
                productGroupName = syncDto.productGroupName,
                categoryId = syncDto.categoryId,
                baseUnitId = syncDto.baseUnitId,
                lastSyncedAt = OffsetDateTime.now(),
                status = updatedStatus
            ))
            return true
        }

        locationProductCache.create(
            LocationProductInsertDto(
                orgProductId = syncDto.orgProductId,
                productName = syncDto.productName,
                description = syncDto.description,
                productGroupName = syncDto.productGroupName,
                categoryId = syncDto.categoryId,
                baseUnitId = syncDto.baseUnitId,
                status = syncDto.status,
                lastSyncedAt = OffsetDateTime.now()
            )
        )
        return true
    }

    companion object {
        const val BASE_UNIT_CANNOT_CHANGE = "A product's base unit cannot change once it is stocked at a location"
    }

    private fun fieldsMatch(existing: LocationProductEntity, syncDto: LocationProductSyncDto): Boolean =
        StringUtils.isEquivalent(existing.productName, syncDto.productName)
            && StringUtils.isEquivalent(existing.description, syncDto.description)
            && StringUtils.isEquivalent(existing.productGroupName, syncDto.productGroupName)
            && existing.categoryId == syncDto.categoryId
            && existing.baseUnitId == syncDto.baseUnitId
            && existing.status == syncDto.status
}
