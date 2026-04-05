package me.ezra_home.retail_software_solution.organizations.business.unitgroup

import me.ezra_home.retail_software_solution.organizations.business.unitgroup.dto.UnitGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.dto.UnitGroupResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.dto.UnitGroupUpdateDto
import me.ezra_home.retail_software_solution.organizations.model.UnitGroupEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
internal interface UnitGroupMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(unitGroupInsertDto: UnitGroupInsertDto): UnitGroupEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toResponseDto(unitGroupEntity: UnitGroupEntity): UnitGroupResponseDto


    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(unitGroupUpdateDto: UnitGroupUpdateDto, @MappingTarget unitGroupEntity: UnitGroupEntity)
}
