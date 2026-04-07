package me.ezra_home.retail_software_solution.organizations.business.product_category.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.product_category.ProductCategoryCache
import me.ezra_home.retail_software_solution.organizations.business.product_category.ProductCategoryMapper
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
    fun getAllCategoryDtos(): Collection<ProductCategoryDto> = productCategoryCache.getAllCategories()

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getCategoryNamesById(): Map<UUID, String> = productCategoryCache.getCategoryNamesById()

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllCategories(): Collection<ProductCategoryResponseDto> {
        return productCategoryCache.getAllCategories().map { productCategoryMapper.toResponseDto(it) }
    }

    fun createCategory(productCategoryInsertDto: ProductCategoryInsertDto): ProductCategoryResponseDto {
        val categoryName = StringUtils.getValueOrException(productCategoryInsertDto.categoryName, NAME_IS_REQUIRED)
        productCategoryCache.getAllCategories().find { StringUtils.isEquivalent(it.categoryName, categoryName) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, categoryName))}
        val savedDto = productCategoryCache.create(productCategoryInsertDto)
        return productCategoryMapper.toResponseDto(savedDto)
    }

    fun updateCategory(productCategoryUpdateDto: ProductCategoryUpdateDto): ProductCategoryResponseDto {
        validateCategoryUpdate(productCategoryUpdateDto)
        val existing = productCategoryCache.getAllCategories().find { Objects.equals(productCategoryUpdateDto.id, it.id) }
            ?: throw UpdatingNonExistingRecordException()
        val updated = productCategoryUpdateDto.applyTo(existing)
        val savedDto = productCategoryCache.save(updated)
        return productCategoryMapper.toResponseDto(savedDto)
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
        const val NAME_ALREADY_EXISTS = "A category with the name %s already exists."
    }
}
