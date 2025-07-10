package me.ezra_home.retail_software_solution.platform.rest.endpoints.db_migration

import me.ezra_home.retail_software_solution.configuration.security.RtsRoles
import me.ezra_home.retail_software_solution.platform.business.db_migration.DbMigrationHistoryService
import me.ezra_home.retail_software_solution.platform.business.db_migration.DbMigrationService
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationRequestDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationResponseDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationRetryRequestDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.MigrationHistoryResponseDto
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@CrossOrigin
@RestController
@RequestMapping("/secured/db-migrations")
@PreAuthorize("hasRole('${RtsRoles.ROLE_PLATFORM_ADMIN}')")
class DbMigrationEndpoint(
    private val dbMigrationService: DbMigrationService,
    private val dbMigrationHistoryService: DbMigrationHistoryService
) {
    @PostMapping("run")
    fun runMigration(@RequestBody dbMigrationRequestDto: DbMigrationRequestDto): DbMigrationResponseDto =
        dbMigrationService.runSchemaMigration(dbMigrationRequestDto)

    @PostMapping("retry")
    fun retryFailedLocations(
        @RequestBody request: DbMigrationRetryRequestDto
    ): DbMigrationResponseDto {
        return dbMigrationService.retryFailedLocationMigrations(request)
    }

    @GetMapping
    fun getMigrations(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) start: OffsetDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) end: OffsetDateTime
    ): Collection<MigrationHistoryResponseDto> =
        dbMigrationHistoryService.getMigrationHistory(start, end)
}
