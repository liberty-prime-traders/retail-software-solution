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
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class LocationProductService(
    private val locationProductRepository: LocationProductRepository,
    private val locationProductCache: LocationProductCache,
    private val locationProductMapper: LocationProductMapper,
    private val locationProductEnricher: LocationProductEnricher,
    private val organizationProductService: OrganizationProductService,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun guardAllActive(ids: Collection<UUID>) {
        val inactive = locationProductRepository.findAllById(ids)
            .filter { it.status != ProductStatus.ACTIVE }
        if (inactive.isNotEmpty()) throw RtsGenericException(
            "Inactive products are not allowed: ${inactive.joinToString { "${it.referenceNumber}/${it.productName}" }}"
        )
    }

    fun updateLastPurchaseCosts(lastPurchaseCostUpdateDtos: List<LastPurchaseCostUpdateDto>) {
        val updatesByLocationProductId = lastPurchaseCostUpdateDtos.associateBy { it.locationProductId }
        val products = locationProductRepository.findAllById(updatesByLocationProductId.keys)
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        products.forEach { product ->
            val lastPurchaseCostUpdateDto = updatesByLocationProductId.getValue(product.id!!)
            val storageUnitsPerBaseUnit = unitConversionGraph.getSizeInRoot(product.baseUnitId)
            product.lastPurchasePrice = lastPurchaseCostUpdateDto.costPerBaseUnit(storageUnitsPerBaseUnit)
        }
        locationProductRepository.saveAll(products)
        locationProductCache.evictAll()
    }

    fun updateDefaultSalePrices(defaultSalePricesByLocationProductId: Map<UUID, Money>) {
        val products = locationProductRepository.findAllById(defaultSalePricesByLocationProductId.keys)
        if (products.size != defaultSalePricesByLocationProductId.size) throw UpdatingNonExistingRecordException()
        products.forEach { it.defaultSalePrice = defaultSalePricesByLocationProductId.getValue(requireNotNull(it.id)).toBigDecimal() }
        locationProductRepository.saveAll(products)
        locationProductCache.evictAll()
    }

    fun updateProduct(locationProductUpdateDto: LocationProductUpdateDto): LocationProductResponseDto {
        val entity = locationProductRepository.findById(locationProductUpdateDto.id).orElseThrow {
            UpdatingNonExistingRecordException()
        }
        LocationProductValidator.validateProductUpdate(locationProductUpdateDto)
        val updated = locationProductUpdateDto.applyTo(locationProductMapper.toDomainDto(entity))
        return convertToResponseDto(locationProductCache.save(updated))
    }

    fun deactivateProduct(locationProductId: UUID): LocationProductResponseDto {
        val entity = locationProductRepository.findById(locationProductId).orElseThrow {
            UpdatingNonExistingRecordException()
        }
        val saved = locationProductCache.save(locationProductMapper.toDomainDto(entity).copy(status = ProductStatus.DISCONTINUED))
        return convertToResponseDto(saved)
    }

    fun reactivateProduct(locationProductId: UUID): LocationProductResponseDto {
        val entity = locationProductRepository.findById(locationProductId).orElseThrow {
            UpdatingNonExistingRecordException()
        }
        verifyOrgProductIsActive(entity.orgProductId)
        val saved = locationProductCache.save(locationProductMapper.toDomainDto(entity).copy(status = ProductStatus.ACTIVE))
        return convertToResponseDto(saved)
    }

    fun verifyOrgProductIsActive(orgProductId: UUID) {
        organizationProductService.verifyProductIsActive(orgProductId)
    }

    private fun convertToResponseDto(locationProductDto: LocationProductDto): LocationProductResponseDto {
        return locationProductEnricher.convertToResponseDto(listOf(locationProductDto)).first()
    }
}
