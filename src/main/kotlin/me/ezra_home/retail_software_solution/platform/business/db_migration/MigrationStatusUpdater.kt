package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity
import me.ezra_home.retail_software_solution.util.enums.MigrationStatus
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class MigrationStatusUpdater(
  private val dbMigrationCache: DbMigrationCache
) {
  fun markSuccess(migration: DbMigrationEntity, message: String) {
    migration.apply {
      status = MigrationStatus.SUCCESS
      this.message = message.take(100)
      endOn = OffsetDateTime.now()
    }
    dbMigrationCache.upsertDbMigration(migration)
  }

  fun markPartial(migration: DbMigrationEntity, message: String) {
    migration.apply {
      status = MigrationStatus.PARTIAL
      this.message = message.take(100)
      endOn = OffsetDateTime.now()
    }
    dbMigrationCache.upsertDbMigration(migration)
  }

  fun markFailure(migration: DbMigrationEntity, error: Exception) {
    migration.apply {
      status = MigrationStatus.FAILURE
      message = error.message?.take(100) ?: "Unknown error during migration"
      endOn = OffsetDateTime.now()
    }
    dbMigrationCache.upsertDbMigration(migration)
  }

  fun markIgnored(migration: DbMigrationEntity, reason: String) {
    migration.apply {
      status = MigrationStatus.IGNORED
      message = reason.take(100)
      endOn = OffsetDateTime.now()
    }
    dbMigrationCache.upsertDbMigration(migration)
  }
}
