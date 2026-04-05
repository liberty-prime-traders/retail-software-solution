package me.ezra_home.retail_software_solution.organizations.business.tag

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.tag.dto.TagDto
import me.ezra_home.retail_software_solution.organizations.business.tag.mapping.TagMapper
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.TAG])
class TagCache(
    private val tagRepository: TagRepository,
    private val tagMapper: TagMapper
) {

    @Cacheable
    fun getAllTags(): Collection<TagDto> = tagRepository.findAll().map { tagMapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun upsertTag(tagDto: TagDto): TagDto {
        val saved = tagRepository.save(tagMapper.toEntity(tagDto))
        return tagMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun deleteTag(id: UUID) {
        tagRepository.deleteById(id)
    }
}
