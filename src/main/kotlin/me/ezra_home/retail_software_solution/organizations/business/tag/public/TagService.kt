package me.ezra_home.retail_software_solution.organizations.business.tag.public

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.tag.TagCache
import me.ezra_home.retail_software_solution.organizations.business.tag.TagRepository
import me.ezra_home.retail_software_solution.organizations.business.tag.mapping.TagMapper
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class TagService(
    private val tagMapper: TagMapper,
    private val tagCache: TagCache,
    private val tagRepository: TagRepository,
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllTags(): Collection<TagResponseDto> {
        return tagCache.getAllTags().map { tagMapper.toResponseDto(it) }
    }

    fun createTag(tagInsertDto: TagInsertDto): TagResponseDto {
        val tagName = StringUtils.getValueOrException(tagInsertDto.tagName, NAME_IS_REQUIRED)

        tagCache.getAllTags().find { StringUtils.isEquivalent(it.tagName, tagName) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, tagName))}

        val similarTags = tagRepository.findSimilarTags(tagName, SIMILARITY_THRESHOLD)
            .map { tagMapper.toResponseDto(tagMapper.toDomainDto(it)) }

        if (similarTags.isNotEmpty()) {
            throwTagSimilarityException(tagName, similarTags)
        }

        val dto = tagMapper.toDomainDto(tagInsertDto)
        val savedDto = tagCache.upsertTag(dto)
        return tagMapper.toResponseDto(savedDto)
    }

    private fun throwTagSimilarityException(attemptedName: String, similarTags: List<TagResponseDto>) {
        val tagNames = similarTags.joinToString(", ") { "'${it.tagName}'" }
        throw RtsGenericException(
            "Cannot create tag '$attemptedName'. Similar tag(s) already exist: $tagNames." +
                    "Please use an existing tag or choose a more distinct name."
        )
    }

    fun updateTag(tagUpdateDto: TagUpdateDto): TagResponseDto {
        validateTagUpdate(tagUpdateDto)
        val tagToUpdate = tagCache.getAllTags().find { Objects.equals(tagUpdateDto.id, it.id) }
            ?: throw UpdatingNonExistingRecordException()
        tagMapper.partialUpdate(tagUpdateDto, tagToUpdate)
        val updatedDto = tagCache.upsertTag(tagToUpdate)
        return tagMapper.toResponseDto(updatedDto)
    }

    private fun validateTagUpdate(tagUpdateDto: TagUpdateDto) {
        val name = StringUtils.getValueOrException(tagUpdateDto.tagName, NAME_IS_REQUIRED)

        tagCache.getAllTags()
            .find { StringUtils.isEquivalent(it.tagName, name) && it.id != tagUpdateDto.id }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }

        val similarTags = tagRepository.findSimilarTags(name, SIMILARITY_THRESHOLD)
            .filter { it.id != tagUpdateDto.id }
            .map { tagMapper.toResponseDto(tagMapper.toDomainDto(it)) }

        if (similarTags.isNotEmpty()) {
            throwTagSimilarityException(name, similarTags)
        }
    }

    fun deleteTag(id: UUID) {
        tagCache.getAllTags().find { it.id == id }?.apply { tagCache.deleteTag(id) }
    }

    companion object {
        const val NAME_IS_REQUIRED = "A tag must have a name"
        const val NAME_ALREADY_EXISTS = "A tag with the name %s already exists."
        const val SIMILARITY_THRESHOLD = 0.4
    }

}
