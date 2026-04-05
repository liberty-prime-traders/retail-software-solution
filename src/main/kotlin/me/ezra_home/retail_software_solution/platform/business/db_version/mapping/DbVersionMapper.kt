package me.ezra_home.retail_software_solution.platform.business.db_version.mapping

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.platform.business.db_version.dto.DbVersionDto
import me.ezra_home.retail_software_solution.platform.business.db_version.api.DbVersionResponseDto
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class, uses = [DbVersionQualifier::class])
interface DbVersionMapper {
    fun toDomainDto(entity: DbVersionEntity): DbVersionDto

    fun toEntity(dto: DbVersionDto): DbVersionEntity

    @Mapping(source = "prevVersionId", target = "prevVersion", qualifiedBy = [DbVersionNumber::class])
    fun toResponseDto(dto: DbVersionDto): DbVersionResponseDto
}
