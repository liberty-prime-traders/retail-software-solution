package me.ezra_home.retail_software_solution.organizations.business.organization_admin

import me.ezra_home.retail_software_solution.organizations.business.organization_admin.api.OrganizationAdminResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface OrganizationAdminMapper {

    fun toDomainDto(entity: OrganizationAdminEntity): OrganizationAdminDto

    @Mapping(target="adminId", ignore = true)
    fun toEntity(dto: OrganizationAdminDto): OrganizationAdminEntity

    @Mapping(source = "userId", target = "user", qualifiedBy = [FullName::class])
    @Mapping(source = "createdOn", target = "startOn")
    fun toResponseDto(dto: OrganizationAdminDto): OrganizationAdminResponseDto
}
