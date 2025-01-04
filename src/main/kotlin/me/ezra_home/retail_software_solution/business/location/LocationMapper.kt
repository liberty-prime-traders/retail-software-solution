package me.ezra_home.retail_software_solution.business.location

import me.ezra_home.retail_software_solution.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.business.location.dto.LocationResponseDto
import me.ezra_home.retail_software_solution.business.location.dto.LocationUpdateDto
import me.ezra_home.retail_software_solution.business.util.mappers.userinfo.CreatedBy
import me.ezra_home.retail_software_solution.business.util.mappers.userinfo.FullName
import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.model.entity.LocationEntity
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
abstract class LocationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @BeanMapping(qualifiedBy = [CreatedBy::class])
    abstract fun toEntity(locationInsertDto: LocationInsertDto): LocationEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    abstract fun toResponseDto(locationEntity: LocationEntity): LocationResponseDto

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    abstract fun partialUpdate(locationUpdateDto: LocationUpdateDto, @MappingTarget locationEntity: LocationEntity)
}
