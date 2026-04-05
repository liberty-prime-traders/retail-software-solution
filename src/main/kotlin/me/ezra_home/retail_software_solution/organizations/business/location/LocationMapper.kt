package me.ezra_home.retail_software_solution.organizations.business.location

import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationDto
import me.ezra_home.retail_software_solution.organizations.business.location.public.LocationInsertDto
import me.ezra_home.retail_software_solution.organizations.business.location.public.LocationResponseDto
import me.ezra_home.retail_software_solution.organizations.business.location.public.LocationUpdateDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
interface LocationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "schemaName", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toDomainDto(locationInsertDto: LocationInsertDto): LocationDto

    fun toDomainDto(locationEntity: LocationEntity): LocationDto

    fun toEntity(locationDto: LocationDto): LocationEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toResponseDto(locationDto: LocationDto): LocationResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "schemaName", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(locationUpdateDto: LocationUpdateDto, @MappingTarget locationDto: LocationDto)
}
