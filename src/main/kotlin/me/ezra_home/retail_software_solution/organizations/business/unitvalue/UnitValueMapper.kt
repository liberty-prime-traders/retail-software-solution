package me.ezra_home.retail_software_solution.organizations.business.unitvalue

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto.UnitValueInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto.UnitValueResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto.UnitValueUpdateDto
import me.ezra_home.retail_software_solution.organizations.model.UnitValueEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class, uses = [UnitValueQualifier::class])
abstract class UnitValueMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    abstract fun toEntity(unitValueInsertDto: UnitValueInsertDto): UnitValueEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = "baseUnit", target = "baseUnitName", qualifiedBy = [UnitName::class])
    abstract fun toResponseDto(unitValueEntity: UnitValueEntity): UnitValueResponseDto

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    abstract fun partialUpdate(unitValueUpdateDto: UnitValueUpdateDto, @MappingTarget unitValueEntity: UnitValueEntity)
}
