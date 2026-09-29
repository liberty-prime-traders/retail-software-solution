package me.ezra_home.retail_software_solution.platform.business.tax_type

import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxTypeDto
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxTypeInsertDto
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxTypeResponseDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface TaxTypeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(dto: TaxTypeInsertDto): TaxTypeEntity

    fun toDomainDto(entity: TaxTypeEntity): TaxTypeDto

    fun toEntity(dto: TaxTypeDto): TaxTypeEntity

    fun toResponseDto(dto: TaxTypeDto): TaxTypeResponseDto
}
