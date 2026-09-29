package me.ezra_home.retail_software_solution.platform.business.jurisdiction_type

import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.api.JurisdictionTypeDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.api.JurisdictionTypeInsertDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.api.JurisdictionTypeResponseDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface JurisdictionTypeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(dto: JurisdictionTypeInsertDto): JurisdictionTypeEntity

    fun toDomainDto(entity: JurisdictionTypeEntity): JurisdictionTypeDto

    fun toEntity(dto: JurisdictionTypeDto): JurisdictionTypeEntity

    fun toResponseDto(dto: JurisdictionTypeDto): JurisdictionTypeResponseDto
}
