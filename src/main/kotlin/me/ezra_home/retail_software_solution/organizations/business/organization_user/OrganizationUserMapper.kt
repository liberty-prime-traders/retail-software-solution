package me.ezra_home.retail_software_solution.organizations.business.organization_user

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.organizations.model.OrganizationUserEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface OrganizationUserMapper {
    @Mapping(source = "userId", target = "user", qualifiedBy = [FullName::class])
    @Mapping(source = "createdOn", target = "startOn")
    fun toDto(entity: OrganizationUserEntity): OrganizationUserResponseDto
}
