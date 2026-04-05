package me.ezra_home.retail_software_solution.platform.business.organization

import me.ezra_home.retail_software_solution.platform.business.authorization_pass.dto.AuthorizationPassQualifier
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.dto.PassReferenceNumber
import me.ezra_home.retail_software_solution.platform.business.db_version.mapping.DbVersionNumber
import me.ezra_home.retail_software_solution.platform.business.db_version.mapping.DbVersionQualifier
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationDto
import me.ezra_home.retail_software_solution.platform.business.organization.`public`.OrganizationInsertDto
import me.ezra_home.retail_software_solution.platform.business.organization.`public`.OrganizationResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization.`public`.OrganizationUpdateDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationEntity
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(
    config = RtsMapperConfig::class,
    uses = [AuthorizationPassQualifier::class, DbVersionQualifier::class]
)
interface OrganizationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "schemaName", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "currentDbVersionId", ignore = true)
    @Mapping(target = "creationPassId", ignore = true)
    fun toDomainDto(dto: OrganizationInsertDto): OrganizationDto

    fun toDomainDto(entity: OrganizationEntity): OrganizationDto

    fun toEntity(dto: OrganizationDto): OrganizationEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = "currentDbVersionId", target = "currentDbVersion", qualifiedBy = [DbVersionNumber::class])
    @Mapping(source = "creationPassId", target = "creationPassReferenceNumber", qualifiedBy = [PassReferenceNumber::class])
    fun toResponseDto(dto: OrganizationDto): OrganizationResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "schemaName", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "currentDbVersionId", ignore = true)
    @Mapping(target = "creationPassId", ignore = true)
    @Mapping(target = "subdomain", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(dto: OrganizationUpdateDto, @MappingTarget organizationDto: OrganizationDto)
}
