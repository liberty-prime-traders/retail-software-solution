package me.ezra_home.retail_software_solution.organizations.business.category

import java.util.UUID
import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.configuration.cache.SchemaLevel
import me.ezra_home.retail_software_solution.organizations.model.CategoryEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.CATEGORY])
class CategoryCache(private val categoryRepository: CategoryRepository) {

    @Cacheable
    fun getAllCategories(): Collection<CategoryEntity> = categoryRepository.findAll()

    @CacheEvict(allEntries = true)
    fun upsertCategories(categoryEntity: CategoryEntity): CategoryEntity = categoryRepository.save(categoryEntity)

    @CacheEvict(allEntries = true)
    fun deleteCategory(id: UUID) {
        categoryRepository.deleteById(id)
    }
}
