package me.ezra_home.retail_software_solution.platform.business.tax_type.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxTypeCache
import org.springframework.stereotype.Service

@Service
@TransactionalOnPlatformSchema(readOnly = true)
class TaxTypeFetcher(private val taxTypeCache: TaxTypeCache) {
    fun getAllDtos(): Collection<TaxTypeDto> = taxTypeCache.getAll()
}
