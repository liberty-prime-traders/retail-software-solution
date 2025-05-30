package me.ezra_home.retail_software_solution.organizations.business.category.mapping

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.category.CategoryCache
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnOrganizationSchema
class CategoryNameQualifier(private val categoryCache: CategoryCache,) {

    @CategoryName
    fun resolveCategoryName(categoryId: UUID?): String? {
        val categoriesById = categoryCache.getCategoriesById()
        return categoryId?.let { categoriesById[it]?.categoryName }
    }
}
