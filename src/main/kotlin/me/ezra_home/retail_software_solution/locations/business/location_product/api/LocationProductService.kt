package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductCache
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductDto
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductEnricher
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductMapper
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductValidator
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductService
import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class LocationProductService(
    private val locationProductRepository: LocationProductRepository,
    private val locationProductCache: LocationProductCache,
    private val locationProductMapper: LocationProductMapper,
    private val locationProductEnricher: LocationProductEnricher,
    private val organizationProductService: OrganizationProductService
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun findSummaryByIds(ids: Collection<UUID>): Map<UUID, LocationProductSummaryDto> =
        locationProductRepository.findAllById(ids).associate { entity ->
            entity.id!! to LocationProductSummaryDto(
                id = entity.id!!,
                referenceNumber = entity.referenceNumber!!,
                productName = entity.productName,
                productGroupName = entity.productGroupName,
                baseUnitId = entity.baseUnitId
            )
        }

    fun updateLastPurchasePrices(prices: Map<UUID, BigDecimal>) {
        val products = locationProductRepository.findAllById(prices.keys)
        products.forEach { it.lastPurchasePrice = prices[it.id] }
        locationProductRepository.saveAll(products)
        locationProductCache.evictAll()
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun findAllProducts(): List<LocationProductResponseDto> {
        return locationProductEnricher.provideMappingContext(locationProductCache.findAllLocationProducts())
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun countAllProducts(): Long = locationProductCache.countAllLocationProducts()

    fun updateProduct(dto: LocationProductUpdateDto): LocationProductResponseDto {
        val entity = locationProductRepository.findById(dto.id).orElseThrow {
            UpdatingNonExistingRecordException()
        }
        LocationProductValidator.validateProductUpdate(dto)
        val updated = dto.applyTo(locationProductMapper.toDomainDto(entity))
        return convertToResponseDto(locationProductCache.save(updated))
    }

    fun deactivateProduct(productId: UUID): LocationProductResponseDto {
        val entity = locationProductRepository.findById(productId).orElseThrow {
            UpdatingNonExistingRecordException()
        }
        val saved = locationProductCache.save(locationProductMapper.toDomainDto(entity).copy(status = ProductStatus.DISCONTINUED))
        return convertToResponseDto(saved)
    }

    fun reactivateProduct(productId: UUID): LocationProductResponseDto {
        val entity = locationProductRepository.findById(productId).orElseThrow {
            UpdatingNonExistingRecordException()
        }
        verifyOrgProductIsActive(entity.productId)
        val saved = locationProductCache.save(locationProductMapper.toDomainDto(entity).copy(status = ProductStatus.ACTIVE))
        return convertToResponseDto(saved)
    }

    fun verifyOrgProductIsActive(orgProductId: UUID) {
        organizationProductService.verifyProductIsActive(orgProductId)
    }

    private fun convertToResponseDto(dto: LocationProductDto): LocationProductResponseDto {
        return locationProductEnricher.provideMappingContext(listOf(dto)).first()
    }
}
