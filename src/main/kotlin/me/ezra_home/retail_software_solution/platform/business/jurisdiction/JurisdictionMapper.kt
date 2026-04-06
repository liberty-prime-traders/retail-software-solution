package me.ezra_home.retail_software_solution.platform.business.jurisdiction

import me.ezra_home.retail_software_solution.platform.business.jurisdiction.api.JurisdictionDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.api.JurisdictionInsertDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.api.JurisdictionResponseDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class, uses = [JurisdictionQualifier::class])
interface JurisdictionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(dto: JurisdictionInsertDto): JurisdictionEntity

    fun toDomainDto(entity: JurisdictionEntity): JurisdictionDto

    fun toEntity(dto: JurisdictionDto): JurisdictionEntity

    @Mapping(source = "jurisdictionTypeId", target = "jurisdictionType", qualifiedBy = [JurisdictionTypeName::class])
    @Mapping(source = "parentJurisdictionId", target = "parentJurisdiction", qualifiedBy = [ParentJurisdictionName::class])
    @Mapping(source = "id", target = "taxTypes", qualifiedBy = [JurisdictionTaxTypes::class])
    fun toResponseDto(dto: JurisdictionDto, @Context ctx: JurisdictionMappingContext): JurisdictionResponseDto
}
