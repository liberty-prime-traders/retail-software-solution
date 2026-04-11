package me.ezra_home.retail_software_solution.organizations.business.feature

import me.ezra_home.retail_software_solution.organizations.business.feature.api.OrganizationFeatureResponseDto
import me.ezra_home.retail_software_solution.organizations.business.feature.api.OrganizationFeatureService
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.CreatedBy
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface OrganizationFeatureMapper {
    fun toDomainDto(entity: OrganizationFeatureEntity): OrganizationFeatureDto
    fun toEntity(dto: OrganizationFeatureDto): OrganizationFeatureEntity

    @Mapping(target = "enabledBy", source = "enabledBy", qualifiedBy = [FullName::class])
    @Mapping(target = "disabledBy", source = "disabledBy", qualifiedBy = [FullName::class])
    fun toResponseDto(dto: OrganizationFeatureDto): OrganizationFeatureResponseDto
}
