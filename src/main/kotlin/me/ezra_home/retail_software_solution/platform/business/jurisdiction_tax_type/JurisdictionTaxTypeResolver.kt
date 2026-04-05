package me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type

import me.ezra_home.retail_software_solution.platform.business.jurisdiction.JurisdictionCache
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxTypeCache
import me.ezra_home.retail_software_solution.platform.business.tax_type.`public`.PlatformTaxTypeDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JurisdictionTaxTypeResolver(
    private val jurisdictionTaxTypeCache: JurisdictionTaxTypeCache,
    private val jurisdictionCache: JurisdictionCache,
    private val taxTypeCache: TaxTypeCache
) {

    fun buildIndex(): Map<UUID, PlatformTaxTypeDto> {
        val jurisdictionIndex = jurisdictionCache.getAll().associateBy { it.id }
        val taxTypeIndex = taxTypeCache.getAll().associateBy { it.id }
        return jurisdictionTaxTypeCache.getAll().mapNotNull { link ->
            link.id?.let { id ->
                val jurisdiction = jurisdictionIndex[link.jurisdictionId] ?: return@mapNotNull null
                val taxType = taxTypeIndex[link.taxTypeId] ?: return@mapNotNull null
                id to PlatformTaxTypeDto(taxType.getNullSafeId(), "${jurisdiction.name} - ${taxType.name}")
            }
        }.toMap()
    }
}
