package me.ezra_home.retail_software_solution.organizations.business.product_group

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.model.ProductGroupEntity
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.PRODUCT_GROUP])
class ProductGroupCache(private val productGroupRepository: ProductGroupRepository) {

  @Cacheable
  fun findAllProductGroups(): Collection<ProductGroupEntity> = productGroupRepository.findAll()

  @CacheEvict(allEntries = true)
  fun upsertProductGroup(productGroupEntity: ProductGroupEntity) {
    productGroupRepository.save(productGroupEntity)
  }
}
