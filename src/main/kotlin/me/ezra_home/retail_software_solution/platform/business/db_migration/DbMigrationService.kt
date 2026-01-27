package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationRequestDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationRetryRequestDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.OrganizationMigrationResponseDto
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
