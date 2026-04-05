package me.ezra_home.retail_software_solution.platform.business.jurisdiction_type

import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.dto.JurisdictionTypeDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.api.JurisdictionTypeInsertDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.api.JurisdictionTypeResponseDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.api.JurisdictionTypeUpdateDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.JurisdictionTypeEntity
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
    fun toDomainDto(dto: JurisdictionTypeInsertDto): JurisdictionTypeDto

    fun toDomainDto(entity: JurisdictionTypeEntity): JurisdictionTypeDto

    fun toEntity(dto: JurisdictionTypeDto): JurisdictionTypeEntity

    fun toResponseDto(dto: JurisdictionTypeDto): JurisdictionTypeResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(dto: JurisdictionTypeUpdateDto, @MappingTarget jurisdictionTypeDto: JurisdictionTypeDto)
}
