package me.ezra_home.retail_software_solution.platform.business.db_version

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.business.db_version.api.DbVersionDto
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
    fun getAllDbVersions(): Collection<DbVersionDto> = dbVersionRepository.findAll().map { mapper.toDomainDto(it) }

    @Cacheable
    fun getVersionNumbersById(): Map<UUID, String> =
        getAllDbVersions().associate { it.id to it.versionNumber }

    fun findMaxSequenceNumber(): Long? = dbVersionRepository.findMaxSequenceNumber()

    @CacheEvict(allEntries = true)
    fun save(dto: DbVersionDto): DbVersionDto {
        val saved = dbVersionRepository.save(mapper.toEntity(dto))
        return mapper.toDomainDto(saved)
    }
}
