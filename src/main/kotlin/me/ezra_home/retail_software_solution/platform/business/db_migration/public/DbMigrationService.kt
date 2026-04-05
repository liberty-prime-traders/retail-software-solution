package me.ezra_home.retail_software_solution.platform.business.db_migration.public

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.db_migration.MigrationResponseMapper
import me.ezra_home.retail_software_solution.platform.business.db_migration.MigrationRetryHandler
import me.ezra_home.retail_software_solution.platform.business.db_migration.MigrationValidator
import me.ezra_home.retail_software_solution.platform.business.db_migration.OrganizationMigrationHandler
import org.springframework.stereotype.Service

@TransactionalOnPlatformSchema
@Service
class DbMigrationService(
  private val migrationValidator: MigrationValidator,
  private val organizationMigrationHandler: OrganizationMigrationHandler,
  private val migrationRetryHandler: MigrationRetryHandler,
  private val responseMapper: MigrationResponseMapper,
) {
  fun runSchemaMigration(request: DbMigrationRequestDto): OrganizationMigrationResponseDto {
    val targetDbVersion = migrationValidator.validateMigrationRequest(request)
    val result = organizationMigrationHandler.migrateOrganizationAndLocations(
      organizationId = request.organizationId,
      targetDbVersion = targetDbVersion,
      locationIds = request.locationIdsToMigrate
    )
    return responseMapper.toOrganizationResponse(result)
  }

  fun retryFailedLocationMigrations(request: DbMigrationRetryRequestDto): OrganizationMigrationResponseDto {
    val (originalMigration, targetDbVersion) = migrationValidator.validateRetryRequest(request)
    val result = migrationRetryHandler.retryLocationMigrations(
      originalMigration = originalMigration,
      targetDbVersion = targetDbVersion,
      locationIds = request.locationIdsToMigrate
    )
    return responseMapper.toOrganizationResponse(result)
  }
}
