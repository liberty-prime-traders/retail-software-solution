package me.ezra_home.retail_software_solution.util.business.mappers.category

import me.ezra_home.retail_software_solution.organizations.business.category.CategoryService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CategoryNameQualifier(private val categoryService: CategoryService) {

    @CategoryName
    fun resolveCategoryName(categoryId: UUID?): String? {
        val cachedCategories = categoryService.getCachedCategoryMap()
        return categoryId?.let { cachedCategories[it]?.categoryName }
    }
}