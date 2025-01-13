package me.ezra_home.retail_software_solution.business.category

import java.util.Objects
import me.ezra_home.retail_software_solution.business.category.dto.CategoryInsertDto
import me.ezra_home.retail_software_solution.business.category.dto.CategoryResponseDto
import me.ezra_home.retail_software_solution.business.category.dto.CategoryUpdateDto
import me.ezra_home.retail_software_solution.business.util.exceptions.UpdatingNonExistingRecordException
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
        val newCategoryEntity = categoryMapper.toEntity(categoryInsertDto)
        val savedCategoryEntity = categoryCache.upsertCategories(newCategoryEntity)
        return categoryMapper.toDto(savedCategoryEntity)
    }

    @Transactional
    fun updateCategory(categoryDto: CategoryUpdateDto): CategoryResponseDto {
        val categoryToUpdate = categoryCache.getAllCategories().find { Objects.equals(categoryDto.id, it.id) }
        if (categoryToUpdate == null) throw UpdatingNonExistingRecordException()
        categoryMapper.partialUpdate(categoryDto, categoryToUpdate)
        val updatedCategory = categoryCache.upsertCategories(categoryToUpdate)
        return categoryMapper.toDto(updatedCategory)
    }
}