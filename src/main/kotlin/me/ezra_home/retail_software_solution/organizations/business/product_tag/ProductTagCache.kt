package me.ezra_home.retail_software_solution.organizations.business.product_tag

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.PRODUCT_TAG])
class ProductTagCache(
    private val productTagRepository: ProductTagRepository,
    private val productTagMapper: ProductTagMapper
) {

    @Cacheable
    fun findActiveProductTagsByProductId(productId: UUID): Collection<ProductTagDto> =
        productTagRepository.findActiveProductTagsByProductId(productId).map { productTagMapper.toDomainDto(it) }

    @Cacheable
    fun findActiveTagIdsByProductId(productId: UUID): Collection<UUID> =
        productTagRepository.findActiveTagIdsByProductId(productId)

    fun findActiveProductTagsByProductIds(productIds: Collection<UUID>): List<ProductTagDto> {
        if (productIds.isEmpty()) return emptyList()
        return productTagRepository.findActiveProductTagsByProductIds(productIds).map { productTagMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun saveAllProductTags(productTagDtos: List<ProductTagDto>) {
        productTagRepository.saveAll(productTagDtos.map { productTagMapper.toEntity(it) })
    }
}
