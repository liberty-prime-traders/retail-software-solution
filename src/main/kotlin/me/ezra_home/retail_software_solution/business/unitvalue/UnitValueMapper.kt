package me.ezra_home.retail_software_solution.business.unitvalue

import me.ezra_home.retail_software_solution.business.unitvalue.dto.UnitValueInsertDto
import me.ezra_home.retail_software_solution.business.unitvalue.dto.UnitValueResponseDto
import me.ezra_home.retail_software_solution.business.unitvalue.dto.UnitValueUpdateDto
import me.ezra_home.retail_software_solution.business.util.mappers.userinfo.CreatedBy
import me.ezra_home.retail_software_solution.business.util.mappers.userinfo.FullName
import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.model.entity.UnitValueEntity
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
    @BeanMapping(qualifiedBy = [CreatedBy::class])
    abstract fun toEntity(unitValueInsertDto: UnitValueInsertDto): UnitValueEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = "baseUnit", target = "baseUnitName", qualifiedBy = [BaseUnitName::class])
    abstract fun toResponseDto(unitValueEntity: UnitValueEntity): UnitValueResponseDto

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    abstract fun partialUpdate(unitValueUpdateDto: UnitValueUpdateDto, @MappingTarget unitValueEntity: UnitValueEntity)
}
