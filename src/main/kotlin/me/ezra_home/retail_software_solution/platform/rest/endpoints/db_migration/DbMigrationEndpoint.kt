package me.ezra_home.retail_software_solution.platform.rest.endpoints.db_migration

import me.ezra_home.retail_software_solution.configuration.security.RtsRoles
import me.ezra_home.retail_software_solution.platform.business.db_migration.DbMigrationService
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationRequestDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.MigrationHistoryResponse
import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime

@RestController
@RequestMapping("/secured/db-migrations")
@PreAuthorize("hasRole('${RtsRoles.ROLE_PLATFORM_ADMIN}')")
class MigrationController(private val dbMigrationService: DbMigrationService) {
    @PostMapping("/run")
    fun runMigration(@RequestBody request: DbMigrationRequestDto): DbMigrationEntity? =
        dbMigrationService.runSchemaMigration(request)

    @GetMapping
    fun getMigrations(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) start: OffsetDateTime?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) end: OffsetDateTime?
    ): Collection<MigrationHistoryResponse> =
        dbMigrationService.getMigrationHistory(start, end)
}
