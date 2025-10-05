package me.ezra_home.retail_software_solution.locations.business.prefix_configuration

import me.ezra_home.retail_software_solution.business.prefix_configuration.shared.dto.PrefixConfigurationInsertDto
import me.ezra_home.retail_software_solution.business.prefix_configuration.shared.dto.PrefixConfigurationResponseDto
import me.ezra_home.retail_software_solution.business.prefix_configuration.shared.dto.PrefixConfigurationUpdateDto
import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.locations.model.LocationPrefixConfigurationEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
interface LocationPrefixConfigurationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(dto: PrefixConfigurationInsertDto): LocationPrefixConfigurationEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toDto(entity: LocationPrefixConfigurationEntity): PrefixConfigurationResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun updateEntity(
        dto: PrefixConfigurationUpdateDto,
        @MappingTarget entity: LocationPrefixConfigurationEntity
    ): LocationPrefixConfigurationEntity
}
