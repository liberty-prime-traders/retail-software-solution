package me.ezra_home.retail_software_solution.organizations.business.organization_admin

import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.organizations.model.OrganizationAdminEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface OrganizationAdminMapper {
    @Mapping(source = "userId", target = "admin", qualifiedBy = [FullName::class])
    fun toResponseDto(organizationAdminEntity: OrganizationAdminEntity): OrganizationAdminResponseDto
}
