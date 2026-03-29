package me.ezra_home.retail_software_solution.platform.business.tax_type

import me.ezra_home.retail_software_solution.platform.business.tax_type.dto.TaxTypeInsertDto
import me.ezra_home.retail_software_solution.platform.business.tax_type.dto.TaxTypeResponseDto
import me.ezra_home.retail_software_solution.platform.business.tax_type.dto.TaxTypeUpdateDto
import me.ezra_home.retail_software_solution.platform.model.TaxTypeEntity
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
interface TaxTypeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(dto: TaxTypeInsertDto): TaxTypeEntity

    fun toResponseDto(entity: TaxTypeEntity): TaxTypeResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "calculationMethod", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(dto: TaxTypeUpdateDto, @MappingTarget entity: TaxTypeEntity)
}
