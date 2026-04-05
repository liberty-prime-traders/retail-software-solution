package me.ezra_home.retail_software_solution.organizations.business.product_category

import java.util.UUID
import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.product_category.dto.ProductCategoryDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.mapping.ProductCategoryMapper
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

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
    fun getCategoriesById(): Map<UUID, ProductCategoryDto> {
        return getAllCategories().associateBy { it.id!! }
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
