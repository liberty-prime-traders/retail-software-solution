package me.ezra_home.retail_software_solution.organizations.business.product_group

import me.ezra_home.retail_software_solution.organizations.business.category.CategoryCache
import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupUpdateDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.enums.CategoryType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductGroupValidator(
  private val productGroupCache: ProductGroupCache,
  private val categoryCache: CategoryCache
) {

  fun validateProductGroupUpdate(dto: ProductGroupUpdateDto) {
    val name = StringUtils.getValueOrException(dto.groupName, NAME_IS_REQUIRED)
    productGroupCache.findAllProductGroups()
      .find { StringUtils.isEquivalent(it.groupName, name) && it.id != dto.id }?.let {
        throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name))
    }
    validateProductGroupCategory(dto.categoryId?.get())
  }

  fun validateProductGroupInsert(productGroupInsertDto: ProductGroupInsertDto) {
    val name = StringUtils.getValueOrException(productGroupInsertDto.groupName, NAME_IS_REQUIRED)
    productGroupCache.findAllProductGroups()
      .find { StringUtils.isEquivalent(it.groupName, productGroupInsertDto.groupName) }
      ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name))
    }
    validateProductGroupCategory(productGroupInsertDto.categoryId)
  }

  private fun validateProductGroupCategory(categoryId: UUID?) {
    categoryId?.let {
      val category = categoryCache.getAllCategories().find { it.id == categoryId }
        ?: throw RtsGenericException(INVALID_CATEGORY_ID)
      if (CategoryType.PRODUCT != category.categoryType) {
        throw RtsGenericException(CATEGORY_TYPE_MUST_BE_PRODUCT)
      }
    }
  }

  companion object {
    const val NAME_IS_REQUIRED = "A product group must have a name"
    const val NAME_ALREADY_EXISTS = "A product group with the name %s already exists."
    const val INVALID_CATEGORY_ID = "The category ID provided does not exist."
    const val CATEGORY_TYPE_MUST_BE_PRODUCT = "The category type provided must be PRODUCT."
  }
}
