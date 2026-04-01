package me.ezra_home.retail_software_solution.organizations.business.product_category

import java.util.UUID
import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.organizations.model.ProductCategoryEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.PRODUCT_CATEGORY])
class ProductCategoryCache(private val productCategoryRepository: ProductCategoryRepository) {

    @Cacheable
    fun getAllCategories(): Collection<ProductCategoryEntity> = productCategoryRepository.findAll()

    @Cacheable
    fun getCategoriesById(): Map<UUID, ProductCategoryEntity> {
        return getAllCategories().associateBy { it.getNullSafeId() }
    }

    @CacheEvict(allEntries = true)
    fun upsertCategories(productCategoryEntity: ProductCategoryEntity): ProductCategoryEntity = productCategoryRepository.save(productCategoryEntity)

    @CacheEvict(allEntries = true)
    fun deleteCategory(id: UUID) {
        productCategoryRepository.deleteById(id)
    }
}
