package me.ezra_home.retail_software_solution.organizations.business.product

import java.util.UUID
import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.organizations.model.ProductEntity
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.configuration.cache.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.PRODUCT])
class ProductCache(private val productRepository: ProductRepository) {

    @Cacheable
    fun getAllProducts(): Collection<ProductEntity> = productRepository.findAll()

    @CacheEvict(allEntries = true)
    fun upsertProducts(productEntity: ProductEntity): ProductEntity = productRepository.save(productEntity)

    @CacheEvict(allEntries = true)
    fun deleteProduct(id: UUID) {
        productRepository.deleteById(id)
    }
}