package me.ezra_home.retail_software_solution.organizations.business.product_category

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryDto
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.PRODUCT_CATEGORY])
class ProductCategoryCache(
    private val productCategoryRepository: ProductCategoryRepository,
    private val productCategoryMapper: ProductCategoryMapper
) {

    @Cacheable
    fun getAllCategories(): Collection<ProductCategoryDto> =
        productCategoryRepository.findAll().map { productCategoryMapper.toDomainDto(it) }

    @Cacheable
    fun getCategoryNamesById(): Map<UUID, String> {
        return getAllCategories().associate { it.getNullSafeId() to it.categoryName!! }
    }

    @CacheEvict(allEntries = true)
    fun upsertCategories(productCategoryDto: ProductCategoryDto): ProductCategoryDto {
        val saved = productCategoryRepository.save(productCategoryMapper.toEntity(productCategoryDto))
        return productCategoryMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun deleteCategory(id: UUID) {
        productCategoryRepository.deleteById(id)
    }
}
