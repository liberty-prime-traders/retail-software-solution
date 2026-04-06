package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductCache
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductMapper
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductValidator
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductService
import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueService
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
  private val organizationProductService: OrganizationProductService,
  private val unitValueService: UnitValueService
) {

  @TransactionalOnLocationSchema(readOnly = true)
  fun findSummaryByIds(ids: Collection<UUID>): Map<UUID, LocationProductSummaryDto> {
    return locationProductRepository.findAllById(ids).associate { entity ->
      entity.getNullSafeId() to LocationProductSummaryDto(
        id = entity.getNullSafeId(),
        referenceNumber = entity.getNullSafeReferenceNumber(),
        productName = entity.productName,
        productGroupName = entity.productGroupName,
        baseUnitId = entity.baseUnitId
      )
    }
  }

  fun updateLastPurchasePrices(prices: Map<UUID, BigDecimal>) {
    val products = locationProductRepository.findAllById(prices.keys)
    products.forEach { it.lastPurchasePrice = prices[it.id] }
    locationProductRepository.saveAll(products)
    locationProductCache.evictAll()
  }

  @TransactionalOnLocationSchema(readOnly = true)
  fun findAllProducts(): List<LocationProductResponseDto> {
    val unitNamesById = unitValueService.getUnitNamesById()
    return locationProductCache.findAllLocationProducts().map { locationProductMapper.toDto(it, unitNamesById[it.baseUnitId]) }
  }

  @TransactionalOnLocationSchema(readOnly = true)
  fun countAllProducts(): Long = locationProductCache.countAllLocationProducts()

  fun updateProduct(dto: LocationProductUpdateDto): LocationProductResponseDto {
    val entity = locationProductRepository.findById(dto.id).orElseThrow {
      UpdatingNonExistingRecordException()
    }
    LocationProductValidator.validateProductUpdate(dto)
    val productDto = locationProductMapper.toDomainDto(entity)
    locationProductMapper.partialUpdate(dto, productDto)
    locationProductCache.upsertLocationProduct(productDto)
    return locationProductMapper.toDto(productDto, unitValueService.getUnitName(productDto.baseUnitId))
  }

  fun deactivateProduct(productId: UUID): LocationProductResponseDto {
    val entity = locationProductRepository.findById(productId).orElseThrow {
      UpdatingNonExistingRecordException()
    }
    val productDto = locationProductMapper.toDomainDto(entity)
    productDto.status = ProductStatus.DISCONTINUED
    locationProductCache.upsertLocationProduct(productDto)
    return locationProductMapper.toDto(productDto, unitValueService.getUnitName(productDto.baseUnitId))
  }

  fun reactivateProduct(productId: UUID): LocationProductResponseDto {
    val entity = locationProductRepository.findById(productId).orElseThrow {
      UpdatingNonExistingRecordException()
    }
    verifyOrgProductIsActive(entity.productId)
    val productDto = locationProductMapper.toDomainDto(entity)
    productDto.status = ProductStatus.ACTIVE
    locationProductCache.upsertLocationProduct(productDto)
    return locationProductMapper.toDto(productDto, unitValueService.getUnitName(productDto.baseUnitId))
  }

  fun verifyOrgProductIsActive(orgProductId: UUID) {
    organizationProductService.verifyProductIsActive(orgProductId)
  }

}
