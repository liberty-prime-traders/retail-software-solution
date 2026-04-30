package me.ezra_home.retail_software_solution.platform.business.organization

import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionNumber
import me.ezra_home.retail_software_solution.platform.business.db_version.api.DbVersionService
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationDto
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationInsertDto
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(
    config = RtsMapperConfig::class,
    uses = [AuthorizationPassQualifier::class, DbVersionService::class]
)
interface OrganizationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "schemaName", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "currentDbVersionId", ignore = true)
    @Mapping(target = "creationPassId", ignore = true)
    fun toEntity(dto: OrganizationInsertDto): OrganizationEntity

    fun toDomainDto(entity: OrganizationEntity): OrganizationDto

    fun toEntity(dto: OrganizationDto): OrganizationEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = "currentDbVersionId", target = "currentDbVersion", qualifiedBy = [DbVersionNumber::class])
    @Mapping(source = "creationPassId", target = "creationPassReferenceNumber", qualifiedBy = [PassReferenceNumber::class])
    fun toResponseDto(dto: OrganizationDto): OrganizationResponseDto
}
