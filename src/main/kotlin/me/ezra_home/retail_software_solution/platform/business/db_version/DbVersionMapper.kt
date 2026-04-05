package me.ezra_home.retail_software_solution.platform.business.db_version

import me.ezra_home.retail_software_solution.platform.business.db_version.api.DbVersionResponseDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface DbVersionMapper {
    fun toDomainDto(entity: DbVersionEntity): DbVersionDto

    fun toEntity(dto: DbVersionDto): DbVersionEntity

    @Mapping(target = "prevVersion", expression = "java(prevVersion)")
    fun toResponseDto(dto: DbVersionDto, @Context prevVersion: String?): DbVersionResponseDto
}
