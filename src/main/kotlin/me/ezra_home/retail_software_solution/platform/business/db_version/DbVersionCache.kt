package me.ezra_home.retail_software_solution.platform.business.db_version

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.model.DbVersionEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheConfig(cacheNames = [CacheNames.DB_VERSION])
class DbVersionCache(private val dbVersionRepository: DbVersionRepository) {

    @Cacheable
    fun getAllDbVersions(): Collection<DbVersionEntity> {
        return dbVersionRepository.findAll()
    }

   @Cacheable
    fun getLatestDbVersion(): DbVersionEntity? {
        return dbVersionRepository.findTopByOrderBySequenceNumberDesc()
    }

    @CacheEvict(allEntries = true)
    fun upsertDbVersion(dbVersionEntity: DbVersionEntity) {
        dbVersionRepository.save(dbVersionEntity)
    }

}
