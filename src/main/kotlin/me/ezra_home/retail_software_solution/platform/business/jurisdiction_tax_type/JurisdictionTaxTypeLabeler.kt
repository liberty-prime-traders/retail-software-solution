package me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type

import me.ezra_home.retail_software_solution.platform.business.jurisdiction.JurisdictionCache
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxTypeCache
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JurisdictionTaxTypeLabeler(
    private val jurisdictionTaxTypeCache: JurisdictionTaxTypeCache,
    private val jurisdictionCache: JurisdictionCache,
    private val taxTypeCache: TaxTypeCache
) {

    fun buildLabelIndex(): Map<UUID, String> {
        val jurisdictionIndex = jurisdictionCache.getAll().associateBy { it.id }
        val taxTypeIndex = taxTypeCache.getAll().associateBy { it.id }
        return jurisdictionTaxTypeCache.getAll().mapNotNull { link ->
            link.id?.let { id ->
                id to "${jurisdictionIndex[link.jurisdictionId]?.name} - ${taxTypeIndex[link.taxTypeId]?.name}"
            }
        }.toMap()
    }
}
