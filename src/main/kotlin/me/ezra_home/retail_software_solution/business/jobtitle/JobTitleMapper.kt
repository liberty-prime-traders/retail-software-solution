package me.ezra_home.retail_software_solution.business.jobtitle

import me.ezra_home.retail_software_solution.business.jobtitle.dto.JobTitleInsertDto
import me.ezra_home.retail_software_solution.business.jobtitle.dto.JobTitleResponseDto
import me.ezra_home.retail_software_solution.business.jobtitle.dto.JobTitleUpdateDto
import me.ezra_home.retail_software_solution.business.util.mappers.userinfo.CreatedBy
import me.ezra_home.retail_software_solution.business.util.mappers.userinfo.FullName
import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.model.entity.JobTitleEntity
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy


@Mapper(config = RtsMapperConfig::class)
interface JobTitleMapper {
    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toDto(titleEntity: JobTitleEntity): JobTitleResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @BeanMapping(qualifiedBy = [CreatedBy::class])
    fun toEntity(titleInsertDto: JobTitleInsertDto): JobTitleEntity

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(titleUpdateDto: JobTitleUpdateDto, @MappingTarget titleEntity: JobTitleEntity)
}