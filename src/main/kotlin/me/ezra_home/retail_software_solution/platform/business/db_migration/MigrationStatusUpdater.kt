package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.platform.business.db_migration.api.MigrationStatus
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class MigrationStatusUpdater(
    private val dbMigrationCache: DbMigrationCache
) {
    fun markSuccess(migration: DbMigrationDto, message: String): DbMigrationDto =
        dbMigrationCache.save(migration.copy(
            status = MigrationStatus.SUCCESS,
            message = message.take(100),
            endOn = OffsetDateTime.now()
        ))

    fun markPartial(migration: DbMigrationDto, message: String): DbMigrationDto =
        dbMigrationCache.save(migration.copy(
            status = MigrationStatus.PARTIAL,
            message = message.take(100),
            endOn = OffsetDateTime.now()
        ))

    fun markFailure(migration: DbMigrationDto, error: Exception): DbMigrationDto =
        dbMigrationCache.save(migration.copy(
            status = MigrationStatus.FAILURE,
            message = error.message?.take(100) ?: "Unknown error during migration",
            endOn = OffsetDateTime.now()
        ))

    fun markIgnored(migration: DbMigrationDto, reason: String): DbMigrationDto =
        dbMigrationCache.save(migration.copy(
            status = MigrationStatus.IGNORED,
            message = reason.take(100),
            endOn = OffsetDateTime.now()
        ))
}
