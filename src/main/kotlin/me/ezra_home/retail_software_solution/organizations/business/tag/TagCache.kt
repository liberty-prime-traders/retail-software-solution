package me.ezra_home.retail_software_solution.organizations.business.tag

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.model.TagEntity
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.TAG])
class TagCache(private val tagRepository: TagRepository) {

    @Cacheable
    fun getAllTags(): Collection<TagEntity> = tagRepository.findAll()

    @CacheEvict(allEntries = true)
    fun upsertTag(tagEntity: TagEntity): TagEntity = tagRepository.save(tagEntity)

    @CacheEvict(allEntries = true)
    fun deleteTag(id: UUID) {
        tagRepository.deleteById(id)
    }
}
