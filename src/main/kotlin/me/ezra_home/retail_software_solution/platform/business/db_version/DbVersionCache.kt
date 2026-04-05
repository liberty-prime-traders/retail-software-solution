package me.ezra_home.retail_software_solution.platform.business.db_version

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.business.db_version.dto.DbVersionDto
import me.ezra_home.retail_software_solution.platform.business.db_version.mapping.DbVersionMapper
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheConfig(cacheNames = [CacheNames.DB_VERSION])
internal class DbVersionCache(
    private val dbVersionRepository: DbVersionRepository,
    private val mapper: DbVersionMapper
) {

    @Cacheable
    fun getAllDbVersions(): Collection<DbVersionDto> {
        return dbVersionRepository.findAll().map { mapper.toDomainDto(it) }
    }

    fun findMaxSequenceNumber(): Long? {
        return dbVersionRepository.findMaxSequenceNumber()
    }

    @CacheEvict(allEntries = true)
    fun upsertDbVersion(dto: DbVersionDto) {
        dbVersionRepository.save(mapper.toEntity(dto))
    }

}
