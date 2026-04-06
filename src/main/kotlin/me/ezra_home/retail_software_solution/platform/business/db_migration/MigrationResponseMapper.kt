package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.platform.business.db_migration.api.OrganizationMigrationResponseDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.mapping.DbMigrationMapper
import org.springframework.stereotype.Component

@Component
class MigrationResponseMapper(private val dbMigrationMapper: DbMigrationMapper) {

  fun toOrganizationResponse(migration: OrganizationLocationsMigration): OrganizationMigrationResponseDto {
    return dbMigrationMapper.toOrganizationResponseDto(migration.organizationMigration).apply {
      locations = migration.locationMigrations.map {
        dbMigrationMapper.toLocationResponseDto(it)
      }
    }
  }
}
