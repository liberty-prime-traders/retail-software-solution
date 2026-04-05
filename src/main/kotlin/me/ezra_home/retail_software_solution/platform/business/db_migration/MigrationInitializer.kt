package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationDto
import me.ezra_home.retail_software_solution.platform.business.db_version.dto.DbVersionDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationDto
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class MigrationInitializer(private val dbMigrationCache: DbMigrationCache) {

  fun createOrganizationMigration(
    organization: OrganizationDto,
    targetDbVersion: DbVersionDto,
    migrationType: MigrationType,
    message: String
  ): DbMigrationDto {
    return DbMigrationDto(
      dbVersionId = targetDbVersion.getNullSafeId(),
      schemaOwnerId = organization.getNullSafeId(),
      schemaOwnerType = SchemaOwnerType.ORGANIZATION,
      status = MigrationStatus.INITIATED,
      migrationType = migrationType,
      message = message
    ).also { dbMigrationCache.upsertDbMigration(it) }
  }

  fun createLocationMigration(
    location: LocationDto,
    targetDbVersion: DbVersionDto,
    parentMigrationId: UUID
  ): DbMigrationDto {
    return DbMigrationDto(
      dbVersionId = targetDbVersion.getNullSafeId(),
      schemaOwnerId = location.getNullSafeId(),
      schemaOwnerType = SchemaOwnerType.LOCATION,
      status = MigrationStatus.INITIATED,
      migrationType = null,
      message = "Location migration in progress",
      migrationParentId = parentMigrationId
    ).also { dbMigrationCache.upsertDbMigration(it) }
  }

  fun createRetryMigration(
    originalMigration: DbMigrationDto,
    targetDbVersion: DbVersionDto
  ): DbMigrationDto {
    return DbMigrationDto(
      dbVersionId = targetDbVersion.getNullSafeId(),
      schemaOwnerId = originalMigration.schemaOwnerId,
      schemaOwnerType = SchemaOwnerType.ORGANIZATION,
      status = MigrationStatus.INITIATED,
      message = "Retry of failed locations from migration ${originalMigration.id}",
      migrationParentId = originalMigration.getNullSafeId(),
      migrationType = MigrationType.LOCATIONS_ONLY
    ).also { dbMigrationCache.upsertDbMigration(it) }
  }
}
