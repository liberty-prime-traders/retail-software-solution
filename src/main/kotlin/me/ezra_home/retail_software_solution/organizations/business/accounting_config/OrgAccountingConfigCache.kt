package me.ezra_home.retail_software_solution.organizations.business.accounting_config

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigDto
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigInsertDto
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ORG_ACCOUNTING_CONFIG])
class OrgAccountingConfigCache(
    private val repository: OrgAccountingConfigRepository,
    private val mapper: OrgAccountingConfigMapper
) {

    //@Cacheable
    fun get(): OrgAccountingConfigDto? = repository.findAll()
        .firstOrNull()
        ?.let { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun create(insertDto: OrgAccountingConfigInsertDto): OrgAccountingConfigDto {
        val saved = repository.saveAndFlush(mapper.toEntity(insertDto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(dto: OrgAccountingConfigDto): OrgAccountingConfigDto {
        val saved = repository.save(mapper.toEntity(dto))
        return mapper.toDomainDto(saved)
    }
}
