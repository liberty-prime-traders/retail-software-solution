package me.ezra_home.retail_software_solution.organizations.business.jobtitle

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.dto.JobTitleDto
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.api.JobTitleInsertDto
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.api.JobTitleResponseDto
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.api.JobTitleUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.JobTitleEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy


@Mapper(config = RtsMapperConfig::class)
interface JobTitleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toDomainDto(titleInsertDto: JobTitleInsertDto): JobTitleDto

    fun toDomainDto(titleEntity: JobTitleEntity): JobTitleDto

    fun toEntity(titleDto: JobTitleDto): JobTitleEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toDto(titleDto: JobTitleDto): JobTitleResponseDto

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(titleUpdateDto: JobTitleUpdateDto, @MappingTarget titleDto: JobTitleDto)
}
