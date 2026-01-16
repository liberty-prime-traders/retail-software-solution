package me.ezra_home.retail_software_solution.organizations.business.product_group

import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupUpdateDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class ProductGroupValidator(private val productGroupCache: ProductGroupCache) {

  fun validateProductGroupUpdate(dto: ProductGroupUpdateDto) {
    val name = StringUtils.getValueOrException(dto.groupName, NAME_IS_REQUIRED)
    productGroupCache.findAllProductGroups()
      .find { StringUtils.isEquivalent(it.groupName, name) && it.id != dto.id }?.let {
        throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name))
    }
  }

  fun validateProductGroupInsert(productGroupInsertDto: ProductGroupInsertDto) {
    val name = StringUtils.getValueOrException(productGroupInsertDto.groupName, NAME_IS_REQUIRED)
    productGroupCache.findAllProductGroups()
      .find { StringUtils.isEquivalent(it.groupName, productGroupInsertDto.groupName) }
      ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name))
    }
  }

  companion object {
    const val NAME_IS_REQUIRED = "A product group must have a name"
    const val NAME_ALREADY_EXISTS = "A product group with the name %s already exists."
  }
}
