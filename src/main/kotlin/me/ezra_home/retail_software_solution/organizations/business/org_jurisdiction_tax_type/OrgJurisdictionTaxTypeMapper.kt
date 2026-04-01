package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type

import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.dto.OrgJurisdictionTaxTypeInsertDto
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.dto.OrgJurisdictionTaxTypeResponseDto
import me.ezra_home.retail_software_solution.organizations.model.OrgJurisdictionTaxTypeEntity
import me.ezra_home.retail_software_solution.platform.business.tax_type.dto.PlatformTaxTypeDto
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

    @Mapping(target = "platformTaxId", expression = "java(platformTaxType.getId())")
    @Mapping(target = "taxLabel", expression = "java(platformTaxType.getLabel())")
    fun toResponseDto(
        entity: OrgJurisdictionTaxTypeEntity,
        @Context platformTaxType: PlatformTaxTypeDto
    ): OrgJurisdictionTaxTypeResponseDto
}
