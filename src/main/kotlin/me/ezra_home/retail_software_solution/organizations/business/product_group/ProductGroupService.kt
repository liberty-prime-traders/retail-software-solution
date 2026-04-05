package me.ezra_home.retail_software_solution.organizations.business.product_group

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupUpdateDto
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ProductGroupService(
  private val productGroupMapper: ProductGroupMapper,
  private val productGroupCache: ProductGroupCache,
  private val productGroupRepository: ProductGroupRepository,
  private val productGroupValidator: ProductGroupValidator
) {

  @TransactionalOnOrganizationSchema(readOnly = true)
  fun getAllProductGroups(): Collection<ProductGroupResponseDto> {
    return productGroupCache.findAllProductGroups().map { productGroupMapper.toResponseDto(it) }
  }

  fun createProductGroup(productGroupInsertDto: ProductGroupInsertDto): ProductGroupResponseDto {
    productGroupValidator.validateProductGroupInsert(productGroupInsertDto)
    val dto = productGroupMapper.toDomainDto(productGroupInsertDto)
    val savedDto = productGroupCache.upsertProductGroup(dto)
    return productGroupMapper.toResponseDto(savedDto)
  }

  fun updateProductGroup(productGroupUpdateDto: ProductGroupUpdateDto): ProductGroupResponseDto {
    productGroupRepository.findById(productGroupUpdateDto.id).orElseThrow { UpdatingNonExistingRecordException() }
    productGroupValidator.validateProductGroupUpdate(productGroupUpdateDto)
    val dto = productGroupCache.findAllProductGroups().find { it.id == productGroupUpdateDto.id }
        ?: throw UpdatingNonExistingRecordException()
    productGroupMapper.partialUpdate(productGroupUpdateDto, dto)
    val savedDto = productGroupCache.upsertProductGroup(dto)
    return productGroupMapper.toResponseDto(savedDto)
  }

  fun deleteProductGroup(productGroupId: UUID) {
    productGroupCache.deleteProductGroupById(productGroupId)
  }
}
