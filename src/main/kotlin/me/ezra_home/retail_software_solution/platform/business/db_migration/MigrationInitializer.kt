package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.organizations.model.LocationEntity
import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity
import me.ezra_home.retail_software_solution.platform.model.DbVersionEntity
import me.ezra_home.retail_software_solution.platform.model.OrganizationEntity
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MigrationInitializer(private val dbMigrationCache: DbMigrationCache) {

  fun createOrganizationMigration(
    organization: OrganizationEntity,
    targetDbVersion: DbVersionEntity,
    migrationType: MigrationType,
    message: String
  ): DbMigrationEntity {
    return DbMigrationEntity(
      dbVersionId = targetDbVersion.getNullSafeId(),
      schemaOwnerId = organization.getNullSafeId(),
      schemaOwnerType = SchemaOwnerType.ORGANIZATION,
      status = MigrationStatus.INITIATED,
      migrationType = migrationType,
      message = message
    ).also { dbMigrationCache.upsertDbMigration(it) }
  }

  fun createLocationMigration(
    location: LocationEntity,
    targetDbVersion: DbVersionEntity,
    parentMigrationId: UUID
  ): DbMigrationEntity {
    return DbMigrationEntity(
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
    originalMigration: DbMigrationEntity,
    targetDbVersion: DbVersionEntity
  ): DbMigrationEntity {
    return DbMigrationEntity(
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
