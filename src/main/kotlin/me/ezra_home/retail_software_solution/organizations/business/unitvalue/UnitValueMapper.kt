package me.ezra_home.retail_software_solution.organizations.business.unitvalue

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto.UnitValueDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueEntity
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
    @Mapping(target = "referenceNumber", ignore = true)
    abstract fun toDomainDto(unitValueInsertDto: UnitValueInsertDto): UnitValueDto

    abstract fun toDomainDto(unitValueEntity: UnitValueEntity): UnitValueDto

    abstract fun toEntity(unitValueDto: UnitValueDto): UnitValueEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = "baseUnit", target = "baseUnitName", qualifiedBy = [UnitName::class])
    abstract fun toResponseDto(unitValueDto: UnitValueDto): UnitValueResponseDto

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "unitGroupId", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    abstract fun partialUpdate(unitValueUpdateDto: UnitValueUpdateDto, @MappingTarget unitValueDto: UnitValueDto)
}
