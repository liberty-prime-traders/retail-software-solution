package me.ezra_home.retail_software_solution.organizations.business.tag.mapping

import me.ezra_home.retail_software_solution.organizations.business.tag.TagEntity
import me.ezra_home.retail_software_solution.organizations.business.tag.api.TagDto
import me.ezra_home.retail_software_solution.organizations.business.tag.api.TagInsertDto
import me.ezra_home.retail_software_solution.organizations.business.tag.api.TagResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface TagMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(tagInsertDto: TagInsertDto): TagEntity

    fun toDomainDto(tagEntity: TagEntity): TagDto

    fun toEntity(tagDto: TagDto): TagEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toResponseDto(tagDto: TagDto): TagResponseDto
}
