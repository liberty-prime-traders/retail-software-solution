package me.ezra_home.retail_software_solution.organizations.business.unitgroup

import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface UnitGroupMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "systemDefined", constant = "false")
    fun toEntity(unitGroupInsertDto: UnitGroupInsertDto): UnitGroupEntity

    fun toDomainDto(unitGroupEntity: UnitGroupEntity): UnitGroupDto

    fun toEntity(unitGroupDto: UnitGroupDto): UnitGroupEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toResponseDto(unitGroupDto: UnitGroupDto): UnitGroupResponseDto
}
