package me.ezra_home.retail_software_solution.platform.business.organization_join_request

import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.api.OrganizationAdminJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.api.OrganizationJoinRequestInsertDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.api.OrganizationJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.api.OrganizationLaunchResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(insertDto: OrganizationJoinRequestInsertDto): OrganizationJoinRequestEntity

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
