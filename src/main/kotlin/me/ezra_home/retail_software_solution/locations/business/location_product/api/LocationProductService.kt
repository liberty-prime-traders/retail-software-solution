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
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
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
    fun guardAllActive(ids: Collection<UUID>) {
        val inactive = locationProductRepository.findAllById(ids)
            .filter { it.status != ProductStatus.ACTIVE }
        if (inactive.isNotEmpty()) throw RtsGenericException(
            "Inactive products are not allowed: ${inactive.joinToString { "${it.referenceNumber}/${it.productName}" }}"
        )
    }

    fun updateLastPurchasePrices(prices: Map<UUID, BigDecimal>) {
        val products = locationProductRepository.findAllById(prices.keys)
        products.forEach { it.lastPurchasePrice = prices[it.id] }
        locationProductRepository.saveAll(products)
        locationProductCache.evictAll()
    }

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
        return locationProductEnricher.convertToResponseDto(listOf(dto)).first()
    }
}
