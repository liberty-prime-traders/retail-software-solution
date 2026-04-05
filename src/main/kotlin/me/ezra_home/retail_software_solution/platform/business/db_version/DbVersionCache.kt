package me.ezra_home.retail_software_solution.platform.business.db_version

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.DB_VERSION])
class DbVersionCache(
    private val dbVersionRepository: DbVersionRepository,
    private val mapper: DbVersionMapper
) {

    @Cacheable
    fun getAllDbVersions(): Collection<DbVersionDto> {
        return dbVersionRepository.findAll().map { mapper.toDomainDto(it) }
    }

    @Cacheable
    fun getVersionNumbersById(): Map<UUID, String> {
        return dbVersionRepository.findAll()
            .mapNotNull { entity -> entity.id?.let { it to entity.versionNumber } }
            .toMap()
    }

    fun findMaxSequenceNumber(): Long? {
        return dbVersionRepository.findMaxSequenceNumber()
    }

    @CacheEvict(allEntries = true)
    fun upsertDbVersion(dto: DbVersionDto) {
        dbVersionRepository.save(mapper.toEntity(dto))
    }

}
