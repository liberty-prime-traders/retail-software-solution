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
    private val productCache: ProductCache,
    private val categoryCache: CategoryCache
) {

    fun validateProductUpdate(productUpdateDto: ProductUpdateDto) {
        val name = StringUtils.getValueOrException(productUpdateDto.productName, NAME_IS_REQUIRED)
        productCache.getAllProducts()
            .find { StringUtils.isEquivalent(it.productName, name) && it.id != productUpdateDto.id }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }

        if (productUpdateDto.categoryId != null) {
            validateProductCategory(productUpdateDto.categoryId.get())
        }
    }

    fun validateProductInsert(productInsertDto: ProductInsertDto) {
        val name = StringUtils.getValueOrException(productInsertDto.productName, NAME_IS_REQUIRED)
        productCache.getAllProducts()
            .find { StringUtils.isEquivalent(it.productName, name)}
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
        validateProductCategory(productInsertDto.categoryId)
    }

    private fun validateProductCategory(categoryId: UUID?) {
        categoryId?.let {
            val category = categoryCache.getAllCategories().find { it.id == categoryId }
                ?: throw RtsGenericException(INVALID_CATEGORY_ID)
            if (CategoryType.PRODUCT != category.categoryType) {
                throw RtsGenericException(CATEGORY_TYPE_MUST_BE_PRODUCT)
            }
        } ?: throw RtsGenericException(CATEGORY_ID_REQUIRED)
    }

    companion object {
        const val NAME_IS_REQUIRED = "A product must have a name"
        const val NAME_ALREADY_EXISTS = "A product with the name %s already exists."
        const val INVALID_CATEGORY_ID = "The category ID provided does not exist."
        const val CATEGORY_ID_REQUIRED = "The category ID is required."
        const val CATEGORY_TYPE_MUST_BE_PRODUCT = "The category type provided must be PRODUCT."
    }
}
