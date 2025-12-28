package me.ezra_home.retail_software_solution.platform.business.db_version.mapping

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.platform.business.db_version.dto.DbVersionResponseDto
import me.ezra_home.retail_software_solution.platform.model.DbVersionEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class, uses = [DbVersionQualifier::class])
interface DbVersionMapper {
    @Mapping(source = "prevVersionId", target = "prevVersion", qualifiedBy = [DbVersionNumber::class])
    fun toResponseDto(dbVersionEntity: DbVersionEntity): DbVersionResponseDto
}
