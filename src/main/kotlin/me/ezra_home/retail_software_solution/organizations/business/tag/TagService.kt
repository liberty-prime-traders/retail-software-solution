package me.ezra_home.retail_software_solution.organizations.business.tag

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.tag.dto.TagInsertDto
import me.ezra_home.retail_software_solution.organizations.business.tag.dto.TagResponseDto
import me.ezra_home.retail_software_solution.organizations.business.tag.dto.TagUpdateDto
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
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllTags(): Collection<TagResponseDto> {
        return tagCache.getAllTags().map { tagMapper.toDto(it) }
    }

    fun createTag(tagInsertDto: TagInsertDto): TagResponseDto {
        val tagName = StringUtils.getValueOrException(tagInsertDto.tagName, NAME_IS_REQUIRED)
        tagCache.getAllTags().find { StringUtils.isEquivalent(it.tagName, tagName) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, tagName))}
        val newTagEntity = tagMapper.toEntity(tagInsertDto)
        val savedTagEntity = tagCache.upsertTag(newTagEntity)
        return tagMapper.toDto(savedTagEntity)
    }

    fun updateTag(tagDto: TagUpdateDto): TagResponseDto {
        validateTagUpdate(tagDto)
        val tagToUpdate = tagCache.getAllTags().find { Objects.equals(tagDto.id, it.id) }
        if (tagToUpdate == null) throw UpdatingNonExistingRecordException()
        tagMapper.partialUpdate(tagDto, tagToUpdate)
        val updatedTag = tagCache.upsertTag(tagToUpdate)
        return tagMapper.toDto(updatedTag)
    }

    private fun validateTagUpdate(tagUpdateDto: TagUpdateDto) {
        val name = StringUtils.getValueOrException(tagUpdateDto.tagName, NAME_IS_REQUIRED)
        tagCache.getAllTags()
            .find { StringUtils.isEquivalent(it.tagName, name) && it.id != tagUpdateDto.id }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
    }

    fun deleteTag(id: UUID) {
        tagCache.getAllTags().find { it.id == id }?.apply { tagCache.deleteTag(id) }
    }

    companion object {
        const val NAME_IS_REQUIRED = "A tag must have a name"
        const val NAME_ALREADY_EXISTS = "A tag with the name %s already exists."
    }

}
