package me.ezra_home.retail_software_solution.organizations.business.organization_user

import me.ezra_home.retail_software_solution.organizations.business.organization_user.api.OrganizationUserInsertDto
import me.ezra_home.retail_software_solution.organizations.business.organization_user.api.OrganizationUserResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface OrganizationUserMapper {

    fun toDomainDto(entity: OrganizationUserEntity): OrganizationUserDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "endOn", ignore = true)
    fun toEntity(insertDto: OrganizationUserInsertDto): OrganizationUserEntity

    fun toEntity(dto: OrganizationUserDto): OrganizationUserEntity

    @Mapping(source = "userId", target = "user", qualifiedBy = [FullName::class])
    @Mapping(source = "createdOn", target = "startOn")
    fun toDto(dto: OrganizationUserDto): OrganizationUserResponseDto
}
