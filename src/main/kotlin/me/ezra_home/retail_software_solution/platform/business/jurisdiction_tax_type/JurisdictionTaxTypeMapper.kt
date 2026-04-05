package me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type

import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.JurisdictionTaxTypeEntity
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeInsertDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface JurisdictionTaxTypeMapper {

    fun toDomainDto(entity: JurisdictionTaxTypeEntity): JurisdictionTaxTypeDto

    fun toEntity(dto: JurisdictionTaxTypeDto): JurisdictionTaxTypeEntity

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "active", constant = "true")
    fun toDomainDto(insertDto: JurisdictionTaxTypeInsertDto): JurisdictionTaxTypeDto
}
