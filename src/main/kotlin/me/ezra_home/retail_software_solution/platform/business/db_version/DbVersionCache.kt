package me.ezra_home.retail_software_solution.platform.business.db_version

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import org.springframework.cache.annotation.CacheConfig
import org.springframework.stereotype.Component

@Component
@CacheConfig(cacheNames = [CacheNames.DB_VERSION])
class DbVersionCache(private val dbVersionRepository: DbVersionRepository) {
}
