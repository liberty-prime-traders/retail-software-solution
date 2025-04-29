package me.ezra_home.retail_software_solution.platform.business.locationadmin

import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.platform.model.LocationAdminEntity
import me.ezra_home.retail_software_solution.util.business.mappers.userinfo.FullName
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface LocationAdminMapper {
    @Mapping(source = "adminId", target = "admin", qualifiedBy = [FullName::class])
    fun toResponseDto(locationAdminEntity: LocationAdminEntity): LocationAdminResponseDto

}
