package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.platform.business.db_migration.DbMigrationDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.api.DbMigrationRequestDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.api.MigrationStatus
import me.ezra_home.retail_software_solution.platform.business.db_migration.api.DbMigrationRetryRequestDto
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionCache
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionDto
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MigrationValidator(
  private val dbVersionCache: DbVersionCache,
  private val dbMigrationCache: DbMigrationCache,
) {
  fun validateMigrationRequest(request: DbMigrationRequestDto): DbVersionDto {
    val targetVersion = getActiveTargetVersion(request.targetDbVersionId)
    validatePreviousMigrationCompleted(request.organizationId, targetVersion)
    return targetVersion
  }

  fun validateRetryRequest(request: DbMigrationRetryRequestDto): Pair<DbMigrationDto, DbVersionDto> {
    val originalMigration = getOriginalMigration(request.orgMigrationId)
    validateRetryEligibility(originalMigration)
    val targetVersion = getTargetVersion(originalMigration.dbVersionId)
    return Pair(originalMigration, targetVersion)
  }

  private fun getActiveTargetVersion(versionId: UUID): DbVersionDto {
    val version = dbVersionCache.getAllDbVersions().find { it.id == versionId }
      ?: throw RtsGenericException("Target DB version not found")

    version.activatedOn ?: throw RtsGenericException("Target DB version is inactive")

    return version
  }

  private fun getTargetVersion(versionId: UUID): DbVersionDto {
    return dbVersionCache.getAllDbVersions().find { it.id == versionId }
      ?: throw RtsGenericException("Target DB version not found")
  }

  private fun validatePreviousMigrationCompleted(organizationId: UUID, targetVersion: DbVersionDto) {
    val prevVersionId = targetVersion.prevVersionId ?: return

    val prevVersion = dbVersionCache.getAllDbVersions().find { it.id == prevVersionId }
      ?: return

    val previousMigration = dbMigrationCache.getTopBySchemaOwnerIdAndSchemaOwnerTypeAndDbVersionIdOrderByStartOnDesc(
      organizationId,
      SchemaOwnerType.ORGANIZATION,
      prevVersionId
    )

    when {
      previousMigration == null -> throw RtsGenericException(
        "Organization must first migrate to version ${prevVersion.versionNumber}"
      )
      previousMigration.status != MigrationStatus.SUCCESS -> throw RtsGenericException(
        "Previous migration attempt (Version: ${prevVersion.versionNumber}) was not successful."
      )
    }
  }

  private fun getOriginalMigration(migrationId: UUID): DbMigrationDto {
    return dbMigrationCache.getAllDbMigrations().find { it.id == migrationId }
      ?: throw RtsGenericException("Original DB migration not found")
  }

  private fun validateRetryEligibility(migration: DbMigrationDto) {
    if (migration.schemaOwnerType != SchemaOwnerType.ORGANIZATION) {
      throw RtsGenericException("Only organization-level migrations can be retried")
    }
    if (migration.status != MigrationStatus.PARTIAL) {
      throw RtsGenericException("Only partially completed migrations can be retried")
    }
  }
}
