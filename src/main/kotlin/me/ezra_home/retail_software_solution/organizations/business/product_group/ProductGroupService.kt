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
    return productGroupCache.findAllProductGroups().map { productGroupMapper.toDto(it) }
  }

  fun createProductGroup(productGroupInsertDto: ProductGroupInsertDto): ProductGroupResponseDto {
    productGroupValidator.validateProductGroupInsert(productGroupInsertDto)
    val productGroupEntity = productGroupMapper.toEntity(productGroupInsertDto)
    productGroupCache.upsertProductGroup(productGroupEntity)
    return productGroupMapper.toDto(productGroupEntity)
  }

  fun updateProductGroup(productGroupDto: ProductGroupUpdateDto): ProductGroupResponseDto {
    val productGroupToUpdate = productGroupRepository.findById(productGroupDto.id).orElseThrow {
      UpdatingNonExistingRecordException()
    }
    productGroupValidator.validateProductGroupUpdate(productGroupDto)
    productGroupMapper.partialUpdate(productGroupDto, productGroupToUpdate)
    productGroupCache.upsertProductGroup(productGroupToUpdate)
    return productGroupMapper.toDto(productGroupToUpdate)
  }

  fun deleteProductGroup(productGroupId: UUID) {
    productGroupCache.deleteProductGroupById(productGroupId)
  }
}
