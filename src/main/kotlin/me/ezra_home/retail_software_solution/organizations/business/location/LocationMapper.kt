package me.ezra_home.retail_software_solution.organizations.business.location

import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationDto
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationInsertDto
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface LocationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "schemaName", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(locationInsertDto: LocationInsertDto): LocationEntity

    fun toDomainDto(locationEntity: LocationEntity): LocationDto

    fun toEntity(locationDto: LocationDto): LocationEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toResponseDto(locationDto: LocationDto): LocationResponseDto
}
