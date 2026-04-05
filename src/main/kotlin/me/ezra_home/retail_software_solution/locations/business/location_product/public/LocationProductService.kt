package me.ezra_home.retail_software_solution.locations.business.location_product.public

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductCache
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductMapper
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductValidator
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductRepository
import me.ezra_home.retail_software_solution.organizations.business.product.`public`.ProductStatus
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
  private val organizationProductRepository: OrganizationProductRepository
) {

  @TransactionalOnLocationSchema(readOnly = true)
  fun findAllProducts(): List<LocationProductResponseDto> {
    return locationProductCache.findAllLocationProducts().map { locationProductMapper.toDto(it) }
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
    return locationProductMapper.toDto(productDto)
  }

  fun deactivateProduct(productId: UUID): LocationProductResponseDto {
    val entity = locationProductRepository.findById(productId).orElseThrow {
      UpdatingNonExistingRecordException()
    }
    val productDto = locationProductMapper.toDomainDto(entity)
    productDto.status = ProductStatus.DISCONTINUED
    locationProductCache.upsertLocationProduct(productDto)
    return locationProductMapper.toDto(productDto)
  }

  fun reactivateProduct(productId: UUID): LocationProductResponseDto {
    val entity = locationProductRepository.findById(productId).orElseThrow {
      UpdatingNonExistingRecordException()
    }
    verifyOrgProductIsActive(entity.productId)
    val productDto = locationProductMapper.toDomainDto(entity)
    productDto.status = ProductStatus.ACTIVE
    locationProductCache.upsertLocationProduct(productDto)
    return locationProductMapper.toDto(productDto)
  }

  @TransactionalOnOrganizationSchema(readOnly = true)
  fun verifyOrgProductIsActive(orgProductId: UUID) {
    val orgProduct = organizationProductRepository.findById(orgProductId).orElseThrow {
      UpdatingNonExistingRecordException()
    }
    if (orgProduct.status != ProductStatus.ACTIVE) {
      throw RtsGenericException("Cannot reactivate location product: organization product is not active")
    }
  }

}
