package me.ezra_home.retail_software_solution.business.unit

import me.ezra_home.retail_software_solution.business.util.mappers.userinfo.CreatedBy
import me.ezra_home.retail_software_solution.business.util.mappers.userinfo.FullName
import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.business.unit.dto.UnitInsertDto
import me.ezra_home.retail_software_solution.business.unit.dto.UnitUpdateDto
import me.ezra_home.retail_software_solution.business.unit.dto.UnitResponseDto
import me.ezra_home.retail_software_solution.model.entity.UnitEntity
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
interface UnitMapper {
    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toDto(unitEntity:UnitEntity): UnitResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @BeanMapping(qualifiedBy = [CreatedBy::class])
    fun toEntity(unitInsertDto: UnitInsertDto): UnitEntity

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(unitDto: UnitUpdateDto, @MappingTarget unitEntity: UnitEntity)
}