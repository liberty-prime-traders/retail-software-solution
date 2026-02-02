package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.dto.LocationProductResponseDto
import me.ezra_home.retail_software_solution.locations.business.location_product.dto.LocationProductUpdateDto
import me.ezra_home.retail_software_solution.util.enums.ProductStatus
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class LocationProductService(
  private val locationProductRepository: LocationProductRepository,
  private val locationProductCache: LocationProductCache,
  private val locationProductMapper: LocationProductMapper
) {

  @TransactionalOnLocationSchema(readOnly = true)
  fun findAllProducts(): List<LocationProductResponseDto> {
    return locationProductCache.findAllLocationProducts().map { locationProductMapper.toDto(it) }
  }

  @TransactionalOnLocationSchema(readOnly = true)
  fun countAllProducts(): Long = locationProductCache.countAllLocationProducts()

  fun updateProduct(dto: LocationProductUpdateDto): LocationProductResponseDto {
    val productToUpdate = locationProductRepository.findById(dto.id).orElseThrow {
      UpdatingNonExistingRecordException()
    }
    LocationProductValidator.validateProductUpdate(dto)
    locationProductMapper.partialUpdate(dto, productToUpdate)
    locationProductCache.upsertLocationProduct(productToUpdate)
    return locationProductMapper.toDto(productToUpdate)
  }

  fun deactivateProduct(productId: UUID): LocationProductResponseDto {
    val productToDeactivate = locationProductRepository.findById(productId).orElseThrow {
      UpdatingNonExistingRecordException()
    }
    productToDeactivate.status = ProductStatus.DISCONTINUED
    locationProductCache.upsertLocationProduct(productToDeactivate)
    return locationProductMapper.toDto(productToDeactivate)
  }

  fun reactivateProduct(productId: UUID): LocationProductResponseDto {
    val productToReactivate = locationProductRepository.findById(productId).orElseThrow {
      UpdatingNonExistingRecordException()
    }
    productToReactivate.status = ProductStatus.ACTIVE
    locationProductCache.upsertLocationProduct(productToReactivate)
    return locationProductMapper.toDto(productToReactivate)
  }

}
