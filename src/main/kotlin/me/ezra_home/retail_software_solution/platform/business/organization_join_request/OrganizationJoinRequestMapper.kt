package me.ezra_home.retail_software_solution.platform.business.organization_join_request

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.platform.business.organization.`public`.OrganizationResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.`public`.OrganizationAdminJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.`public`.OrganizationJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.`public`.OrganizationLaunchResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.OrganizationJoinRequestEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.NullValueMappingStrategy

@Mapper(
    config = RtsMapperConfig::class,
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT
)
interface OrganizationJoinRequestMapper {

    fun toDomainDto(entity: OrganizationJoinRequestEntity): OrganizationJoinRequestDto

    fun toEntity(dto: OrganizationJoinRequestDto): OrganizationJoinRequestEntity

    fun toLaunchResponse(
        organization: OrganizationResponseDto?,
        isOrganizationAdmin: Boolean,
        accessRequested: Boolean
    ): OrganizationLaunchResponseDto

    @Mapping(source = "subdomain", target = "domain")
    @Mapping(source = "createdOn", target = "requestedDate")
    fun toDto(dto: OrganizationJoinRequestDto): OrganizationJoinRequestResponseDto

    @Mapping(source = "createdById", target = "fullName", qualifiedBy = [FullName::class])
    @Mapping(source = "createdOn", target = "requestedDate")
    fun toAdminDto(dto: OrganizationJoinRequestDto): OrganizationAdminJoinRequestResponseDto

}
