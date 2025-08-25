package me.ezra_home.retail_software_solution.platform.business.db_version.mapping

import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.platform.business.db_version.dto.DbVersionResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.platform.model.DbVersionEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class, uses = [DbVersionQualifier::class])
interface DbVersionMapper {
    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = "prevVersionId", target = "prevVersion", qualifiedBy = [DbVersionNumber::class])
    fun toResponseDto(dbVersionEntity: DbVersionEntity): DbVersionResponseDto
}
