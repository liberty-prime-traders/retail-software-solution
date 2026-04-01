package me.ezra_home.retail_software_solution.platform.business.jurisdiction_type

import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.dto.JurisdictionTypeInsertDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.dto.JurisdictionTypeResponseDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.dto.JurisdictionTypeUpdateDto
import me.ezra_home.retail_software_solution.platform.model.JurisdictionTypeEntity
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
interface JurisdictionTypeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(dto: JurisdictionTypeInsertDto): JurisdictionTypeEntity

    fun toResponseDto(entity: JurisdictionTypeEntity): JurisdictionTypeResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(dto: JurisdictionTypeUpdateDto, @MappingTarget entity: JurisdictionTypeEntity)
}
