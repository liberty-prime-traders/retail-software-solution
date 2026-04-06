package me.ezra_home.retail_software_solution.organizations.business.tax_rate

import me.ezra_home.retail_software_solution.organizations.business.tax_rate.api.TaxRateInsertDto
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.api.TaxRateResponseDto
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

    fun toDomainDto(entity: TaxRateEntity): TaxRateDto

    fun toEntity(taxRateDto: TaxRateDto): TaxRateEntity

    @Mapping(target = "taxLabel", ignore = true)
    @Mapping(target = "parentIsActive", ignore = true)
    fun toResponseDtoBase(taxRateDto: TaxRateDto): TaxRateResponseDto

    fun toResponseDto(taxRateDto: TaxRateDto, taxLabel: String?, parentIsActive: Boolean): TaxRateResponseDto =
        toResponseDtoBase(taxRateDto).copy(taxLabel = taxLabel, parentIsActive = parentIsActive)
}
