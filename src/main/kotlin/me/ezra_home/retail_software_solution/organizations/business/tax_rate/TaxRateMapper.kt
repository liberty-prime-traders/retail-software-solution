package me.ezra_home.retail_software_solution.organizations.business.tax_rate

import me.ezra_home.retail_software_solution.organizations.business.tax_rate.dto.TaxRateInsertDto
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.dto.TaxRateResponseDto
import me.ezra_home.retail_software_solution.organizations.model.TaxRateEntity
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface TaxRateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(dto: TaxRateInsertDto): TaxRateEntity

    @Mapping(target = "taxLabel", ignore = true)
    @Mapping(target = "parentIsActive", ignore = true)
    fun toResponseDtoBase(entity: TaxRateEntity): TaxRateResponseDto

    fun toResponseDto(entity: TaxRateEntity, taxLabel: String?, parentIsActive: Boolean): TaxRateResponseDto =
        toResponseDtoBase(entity).copy(taxLabel = taxLabel, parentIsActive = parentIsActive)
}
