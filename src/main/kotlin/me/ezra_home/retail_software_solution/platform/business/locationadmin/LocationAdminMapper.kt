package me.ezra_home.retail_software_solution.platform.business.locationadmin

import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.platform.model.LocationAdminEntity
import org.mapstruct.Mapper

@Mapper(config = RtsMapperConfig::class)
interface LocationAdminMapper {
    fun toResponseDto(locationAdminEntity: LocationAdminEntity): LocationAdminResponseDto

}
