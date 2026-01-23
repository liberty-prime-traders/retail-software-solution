package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.model.ProductEntity
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.PRODUCT])
class ProductCache(private val productRepository: ProductRepository) {

    @Cacheable
    fun findAllProducts(): List<ProductEntity> = productRepository.findAllProducts()

    @Cacheable
    fun countAllProducts(): Long = productRepository.count()

    @CacheEvict(allEntries = true)
    fun upsertProduct(productEntity: ProductEntity) {
        productRepository.save(productEntity)
    }
}
