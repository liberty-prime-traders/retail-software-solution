package me.ezra_home.retail_software_solution.platform.business.organization

import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationUpsertDto
import me.ezra_home.retail_software_solution.platform.model.OrganizationEntity
import me.ezra_home.retail_software_solution.util.business.mappers.userinfo.CreatedBy
import me.ezra_home.retail_software_solution.util.business.mappers.userinfo.FullName
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
interface OrganizationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @Mapping(target = "schemaName", ignore = true)
    @BeanMapping(qualifiedBy = [CreatedBy::class])
    fun toEntity(organizationUpsertDto: OrganizationUpsertDto): OrganizationEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toResponseDto(organizationEntity: OrganizationEntity): OrganizationResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @Mapping(target = "schemaName", ignore = true)
    @Mapping(target = "subdomain", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(organizationUpsertDto: OrganizationUpsertDto, @MappingTarget organizationEntity: OrganizationEntity)
}
