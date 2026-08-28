package me.ezra_home.retail_software_solution.organizations.business.jobtitle

import me.ezra_home.retail_software_solution.organizations.business.jobtitle.api.JobTitleInsertDto
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.api.JobTitleResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface JobTitleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(titleInsertDto: JobTitleInsertDto): JobTitleEntity

    fun toDomainDto(titleEntity: JobTitleEntity): JobTitleDto

    fun toEntity(titleDto: JobTitleDto): JobTitleEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toDto(titleDto: JobTitleDto): JobTitleResponseDto
}
