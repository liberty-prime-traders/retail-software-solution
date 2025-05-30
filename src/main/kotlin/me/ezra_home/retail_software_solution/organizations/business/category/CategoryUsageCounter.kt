package me.ezra_home.retail_software_solution.organizations.business.category

import me.ezra_home.retail_software_solution.organizations.business.category.CategoryCache
import me.ezra_home.retail_software_solution.organizations.model.CategoryEntity
import me.ezra_home.retail_software_solution.util.business.UsageCounter
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CategoryUsageCounter(private val categoryCache: CategoryCache) : UsageCounter<CategoryEntity> {

    override fun incrementUsageCount(id: UUID?) {
        categoryCache.getAllCategories().find { it.id == id }?.let {
            it.usageCount = it.usageCount.plus(1L)
            categoryCache.upsertCategories(it)
        }
    }

    override fun decrementUsageCount(id: UUID?) {
        categoryCache.getAllCategories().find { it.id == id }?.let {
            it.usageCount = it.usageCount.minus(1L)
            categoryCache.upsertCategories(it)
        }
    }
}
