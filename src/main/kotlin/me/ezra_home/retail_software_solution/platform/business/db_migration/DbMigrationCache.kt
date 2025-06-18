package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import org.springframework.cache.annotation.CacheConfig
import org.springframework.stereotype.Component

@Component
@CacheConfig(cacheNames = [CacheNames.DB_MIGRATION])
class DbMigrationCache(private val dbMigrationRepository: DbMigrationRepository) {
}
