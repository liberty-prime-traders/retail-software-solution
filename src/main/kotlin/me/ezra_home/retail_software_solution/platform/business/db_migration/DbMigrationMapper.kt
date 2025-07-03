package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationResponseDto
import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface DbMigrationMapper {
    @Mapping(target = "locations", ignore = true)
    fun toResponseDto(dbMigrationEntity: DbMigrationEntity): DbMigrationResponseDto
}
