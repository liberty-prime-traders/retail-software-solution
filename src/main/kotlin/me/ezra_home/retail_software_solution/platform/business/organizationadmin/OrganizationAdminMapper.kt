package me.ezra_home.retail_software_solution.platform.business.organizationadmin

import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.platform.model.OrganizationAdminEntity
import org.mapstruct.Mapper

@Mapper(config = RtsMapperConfig::class)
interface OrganizationAdminMapper {
    fun toResponseDto(organizationAdminEntity: OrganizationAdminEntity): OrganizationAdminResponseDto
}
