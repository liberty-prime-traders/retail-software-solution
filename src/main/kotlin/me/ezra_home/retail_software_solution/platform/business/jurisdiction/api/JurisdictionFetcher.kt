package me.ezra_home.retail_software_solution.platform.business.jurisdiction.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.JurisdictionCache
import org.springframework.stereotype.Service

@Service
@TransactionalOnPlatformSchema(readOnly = true)
class JurisdictionFetcher(private val jurisdictionCache: JurisdictionCache) {
    fun getAllDtos(): Collection<JurisdictionDto> = jurisdictionCache.getAll()
}
