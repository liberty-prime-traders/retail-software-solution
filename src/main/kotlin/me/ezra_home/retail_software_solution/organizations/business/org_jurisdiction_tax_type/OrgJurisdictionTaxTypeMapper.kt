package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type

import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeDto
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeInsertDto
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeResponseDto
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.PlatformTaxTypeDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface OrgJurisdictionTaxTypeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(dto: OrgJurisdictionTaxTypeInsertDto): OrgJurisdictionTaxTypeEntity

    fun toDomainDto(entity: OrgJurisdictionTaxTypeEntity): OrgJurisdictionTaxTypeDto

    fun toEntity(dto: OrgJurisdictionTaxTypeDto): OrgJurisdictionTaxTypeEntity

    @Mapping(target = "platformTaxId", expression = "java(platformTaxType.getId())")
    @Mapping(target = "taxLabel", expression = "java(platformTaxType.getLabel())")
    fun toResponseDto(
        dto: OrgJurisdictionTaxTypeDto,
        @Context platformTaxType: PlatformTaxTypeDto
    ): OrgJurisdictionTaxTypeResponseDto
}
