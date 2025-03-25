package me.ezra_home.retail_software_solution.locations.business.category

import me.ezra_home.retail_software_solution.locations.business.category.dto.CategoryInsertDto
import me.ezra_home.retail_software_solution.locations.business.category.dto.CategoryResponseDto
import me.ezra_home.retail_software_solution.locations.business.category.dto.CategoryUpdateDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import java.util.Objects
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    private val categoryMapper: CategoryMapper,
    private val categoryCache: CategoryCache
) {

    @Transactional
    fun getAllCategories(): Collection<CategoryResponseDto> {
        return categoryCache.getAllCategories().map { categoryMapper.toDto(it) }
    }

    @Transactional
    fun createCategory(categoryInsertDto: CategoryInsertDto): CategoryResponseDto {
        val categoryName = categoryInsertDto.categoryName?.takeIf { it.isNotBlank() }
            ?: throw RtsGenericException(NAME_IS_REQUIRED)

        if (categoryCache.getAllCategories().any { it.categoryName.equals(categoryName, ignoreCase = true) }) {
            throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, categoryName))
        }

        val newCategoryEntity = categoryMapper.toEntity(categoryInsertDto)
        val savedCategoryEntity = categoryCache.upsertCategories(newCategoryEntity)
        return categoryMapper.toDto(savedCategoryEntity)
    }


    @Transactional
    fun updateCategory(categoryDto: CategoryUpdateDto): CategoryResponseDto {
        validateCategoryUpdate(categoryDto)
        val categoryToUpdate = categoryCache.getAllCategories().find { Objects.equals(categoryDto.id, it.id) }
        if (categoryToUpdate == null) throw UpdatingNonExistingRecordException()
        categoryMapper.partialUpdate(categoryDto, categoryToUpdate)
        val updatedCategory = categoryCache.upsertCategories(categoryToUpdate)
        return categoryMapper.toDto(updatedCategory)
    }

    private fun validateCategoryUpdate(categoryUpdateDto: CategoryUpdateDto) {
        val name = categoryUpdateDto.categoryName?.orElse(null)
            ?: throw RtsGenericException(NAME_IS_REQUIRED)

        if (categoryCache.getAllCategories().any {
                it.categoryName.equals(name, ignoreCase = true) && it.id != categoryUpdateDto.id
            }) {
            throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name))
        }
    }

    @Transactional
    fun deleteCategory(id: UUID?) {
        if (id != null) {
            val entity = categoryCache.getAllCategories().find { it.id == id }
            if (entity != null) {
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
