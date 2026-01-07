package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.organizations.business.category.CategoryCache
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductUpdateDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.enums.CategoryType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductValidator(
    private val productRepository: ProductRepository,
    private val categoryCache: CategoryCache
) {

    fun validateProductUpdate(productUpdateDto: ProductUpdateDto) {
        val name = StringUtils.getValueOrException(productUpdateDto.productName, NAME_IS_REQUIRED)
        productUpdateDto.baseUnitId?.isEmpty?.let {
            throw RtsGenericException("A product must have a base unit.")
        }
        productRepository.findFirstByProductNameIgnoreCase(name)?.let {
            if(it.id != productUpdateDto.id) {
                throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name))
            }
        }
        validateProductCategory(productUpdateDto.categoryId?.get())
    }

    private fun validateProductCategory(categoryId: UUID?) {
        categoryId?.let {
            val category = categoryCache.getAllCategories().find { it.id == categoryId }
                ?: throw RtsGenericException(INVALID_CATEGORY_ID)
            if (CategoryType.PRODUCT != category.categoryType) {
                throw RtsGenericException(CATEGORY_TYPE_MUST_BE_PRODUCT)
            }
        }
    }

    fun validateProductInsert(productInsertDto: ProductInsertDto) {
        val name = StringUtils.getValueOrException(productInsertDto.productName, NAME_IS_REQUIRED)
        productRepository.findFirstByProductNameIgnoreCase(name)
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
        validateProductCategory(productInsertDto.categoryId)
    }


    companion object {
        const val NAME_IS_REQUIRED = "A product must have a name"
        const val NAME_ALREADY_EXISTS = "A product with the name %s already exists."
        const val INVALID_CATEGORY_ID = "The category ID provided does not exist."
        const val CATEGORY_TYPE_MUST_BE_PRODUCT = "The category type provided must be PRODUCT."
    }
}
