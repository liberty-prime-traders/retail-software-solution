package me.ezra_home.retail_software_solution.locations.business.category

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.category.dto.CategoryInsertDto
import me.ezra_home.retail_software_solution.locations.business.category.dto.CategoryResponseDto
import me.ezra_home.retail_software_solution.locations.business.category.dto.CategoryUpdateDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class CategoryService(
    private val categoryMapper: CategoryMapper,
    private val categoryCache: CategoryCache
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun getAllCategories(): Collection<CategoryResponseDto> {
        return categoryCache.getAllCategories().map { categoryMapper.toDto(it) }
    }

    fun createCategory(categoryInsertDto: CategoryInsertDto): CategoryResponseDto {
        val categoryName = StringUtils.getValueOrException(categoryInsertDto.categoryName, NAME_IS_REQUIRED)
        categoryCache.getAllCategories().find { StringUtils.isEquivalent(it.categoryName, categoryName) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, categoryName))}
        val newCategoryEntity = categoryMapper.toEntity(categoryInsertDto)
        val savedCategoryEntity = categoryCache.upsertCategories(newCategoryEntity)
        return categoryMapper.toDto(savedCategoryEntity)
    }

    fun updateCategory(categoryDto: CategoryUpdateDto): CategoryResponseDto {
        validateCategoryUpdate(categoryDto)
        val categoryToUpdate = categoryCache.getAllCategories().find { Objects.equals(categoryDto.id, it.id) }
        if (categoryToUpdate == null) throw UpdatingNonExistingRecordException()
        categoryMapper.partialUpdate(categoryDto, categoryToUpdate)
        val updatedCategory = categoryCache.upsertCategories(categoryToUpdate)
        return categoryMapper.toDto(updatedCategory)
    }

    private fun validateCategoryUpdate(categoryUpdateDto: CategoryUpdateDto) {
        val name = StringUtils.getValueOrException(categoryUpdateDto.categoryName, NAME_IS_REQUIRED)
        categoryCache.getAllCategories()
            .find { StringUtils.isEquivalent(it.categoryName, name) && it.id != categoryUpdateDto.id }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
    }

    fun deleteCategory(id: UUID?) {
        id?.let {
            categoryCache.getAllCategories().find { it.id == id }?.let { entity ->
                val usageCount = entity.usageCount
                if (usageCount > 0L) {
                    throw RtsGenericException("Category ${entity.categoryName} has $usageCount usage(s) and cannot be deleted")
                }
                categoryCache.deleteCategory(id)
            }
        }
    }
    
    companion object {
        const val NAME_IS_REQUIRED = "A category must have a name"
        const val NAME_ALREADY_EXISTS = "A category with the name %s is already assigned."
    }
    
}
