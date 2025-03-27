package me.ezra_home.retail_software_solution.platform.business.organizationadmin

import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.platform.business.organizationadmin.dto.OrganizationAdminInsertDto
import me.ezra_home.retail_software_solution.platform.business.organizationadmin.dto.OrganizationAdminResponseDto
import me.ezra_home.retail_software_solution.platform.model.OrganizationAdminEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface OrganizationAdminMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "startOn", ignore = true)
    @Mapping(target = "endOn", ignore = true)
    fun toEntity(organizationInsertDto: OrganizationAdminInsertDto): OrganizationAdminEntity

    fun toResponseDto(organizationAdminEntity: OrganizationAdminEntity): OrganizationAdminResponseDto
}
