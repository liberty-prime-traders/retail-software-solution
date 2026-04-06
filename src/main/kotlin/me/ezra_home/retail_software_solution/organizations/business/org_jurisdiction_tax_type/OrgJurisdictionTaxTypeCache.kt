package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeDto
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ORG_JURISDICTION_TAX_TYPE])
class OrgJurisdictionTaxTypeCache(
    private val orgJurisdictionTaxTypeRepository: OrgJurisdictionTaxTypeRepository,
    private val orgJurisdictionTaxTypeMapper: OrgJurisdictionTaxTypeMapper
) {

    @Cacheable
    fun getAll(): Collection<OrgJurisdictionTaxTypeDto> =
        orgJurisdictionTaxTypeRepository.findAll().map { orgJurisdictionTaxTypeMapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun saveAll(dtos: Collection<OrgJurisdictionTaxTypeDto>) {
        orgJurisdictionTaxTypeRepository.saveAll(dtos.map { orgJurisdictionTaxTypeMapper.toEntity(it) })
    }
}
