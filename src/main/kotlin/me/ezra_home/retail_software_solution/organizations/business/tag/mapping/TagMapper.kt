package me.ezra_home.retail_software_solution.organizations.business.tag.mapping

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.organizations.business.tag.dto.TagDto
import me.ezra_home.retail_software_solution.organizations.business.tag.dto.TagInsertDto
import me.ezra_home.retail_software_solution.organizations.business.tag.dto.TagResponseDto
import me.ezra_home.retail_software_solution.organizations.business.tag.dto.TagUpdateDto
import me.ezra_home.retail_software_solution.organizations.model.TagEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
internal interface TagMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toDomainDto(tagInsertDto: TagInsertDto): TagDto

    fun toDomainDto(tagEntity: TagEntity): TagDto

    fun toEntity(tagDto: TagDto): TagEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toResponseDto(tagDto: TagDto): TagResponseDto

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(tagUpdateDto: TagUpdateDto, @MappingTarget tagDto: TagDto)
}
