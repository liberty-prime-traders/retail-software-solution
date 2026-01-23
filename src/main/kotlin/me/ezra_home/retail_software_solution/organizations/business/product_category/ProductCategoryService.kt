package me.ezra_home.retail_software_solution.organizations.business.product_category

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.product_category.dto.ProductCategoryInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.dto.ProductCategoryResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.dto.ProductCategoryUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.mapping.ProductCategoryMapper
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ProductCategoryService(
    private val productCategoryMapper: ProductCategoryMapper,
    private val productCategoryCache: ProductCategoryCache,
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllCategories(): Collection<ProductCategoryResponseDto> {
        return productCategoryCache.getAllCategories().map { productCategoryMapper.toDto(it) }
    }

    fun createCategory(productCategoryInsertDto: ProductCategoryInsertDto): ProductCategoryResponseDto {
        val categoryName = StringUtils.getValueOrException(productCategoryInsertDto.categoryName, NAME_IS_REQUIRED)
        productCategoryCache.getAllCategories().find { StringUtils.isEquivalent(it.categoryName, categoryName) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, categoryName))}
        val newProductCategoryEntity = productCategoryMapper.toEntity(productCategoryInsertDto)
        val savedProductCategoryEntity = productCategoryCache.upsertCategories(newProductCategoryEntity)
        return productCategoryMapper.toDto(savedProductCategoryEntity)
    }

    fun updateCategory(productCategoryDto: ProductCategoryUpdateDto): ProductCategoryResponseDto {
        validateCategoryUpdate(productCategoryDto)
        val categoryToUpdate = productCategoryCache.getAllCategories().find { Objects.equals(productCategoryDto.id, it.id) }
        if (categoryToUpdate == null) throw UpdatingNonExistingRecordException()
        productCategoryMapper.partialUpdate(productCategoryDto, categoryToUpdate)
        val updatedCategory = productCategoryCache.upsertCategories(categoryToUpdate)
        return productCategoryMapper.toDto(updatedCategory)
    }

    private fun validateCategoryUpdate(productCategoryUpdateDto: ProductCategoryUpdateDto) {
        val name = StringUtils.getValueOrException(productCategoryUpdateDto.categoryName, NAME_IS_REQUIRED)
        productCategoryCache.getAllCategories()
            .find { StringUtils.isEquivalent(it.categoryName, name) && it.id != productCategoryUpdateDto.id }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
    }

    fun deleteCategory(id: UUID) {
        productCategoryCache.deleteCategory(id)
    }

    companion object {
        const val NAME_IS_REQUIRED = "A category must have a name"
        const val NAME_ALREADY_EXISTS = "A category with the name %s is already assigned."
    }

}
