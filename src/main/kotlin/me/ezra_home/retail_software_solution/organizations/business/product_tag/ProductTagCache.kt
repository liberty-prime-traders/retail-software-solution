package me.ezra_home.retail_software_solution.organizations.business.product_tag

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.model.ProductTagEntity
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.PRODUCT_TAG])
class ProductTagCache(private val productTagRepository: ProductTagRepository) {

    @Cacheable
    fun findActiveProductTagsByProductId(productId: UUID): Collection<ProductTagEntity> =
        productTagRepository.findActiveProductTagsByProductId(productId)

    @Cacheable
    fun findActiveTagIdsByProductId(productId: UUID): Collection<UUID> =
        productTagRepository.findActiveTagIdsByProductId(productId)

    fun findActiveProductTagsByProductIds(productIds: Collection<UUID>): List<ProductTagEntity> {
        if (productIds.isEmpty()) return emptyList()
        return productTagRepository.findActiveProductTagsByProductIds(productIds)
    }

    @CacheEvict(allEntries = true)
    fun saveAllProductTags(productTagEntities: List<ProductTagEntity>) {
        productTagRepository.saveAll(productTagEntities)
    }
}
