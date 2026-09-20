package me.ezra_home.retail_software_solution.platform.business.sysuser

import me.ezra_home.retail_software_solution.platform.business.sysuser.api.SysUserInsertDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface SysUserMapper {
    fun toDomainDto(entity: SysUserEntity): SysUserDto

    fun toEntity(dto: SysUserDto): SysUserEntity

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "disabledAt", ignore = true)
    fun toEntity(insertDto: SysUserInsertDto): SysUserEntity
}
