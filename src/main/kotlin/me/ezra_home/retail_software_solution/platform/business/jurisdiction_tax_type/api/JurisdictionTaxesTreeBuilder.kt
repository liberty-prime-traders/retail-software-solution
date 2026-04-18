package me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeFetcher
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.api.JurisdictionDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.api.JurisdictionFetcher
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.JurisdictionTaxTypeCache
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxRecoveryType
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxTypeFetcher
import me.ezra_home.retail_software_solution.util.ui_models.TreeNodeWithData
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema(readOnly = true)
class JurisdictionTaxesTreeBuilder(
    private val orgJurisdictionTaxTypeFetcher: OrgJurisdictionTaxTypeFetcher,
    private val taxTypeFetcher: TaxTypeFetcher,
    private val jurisdictionTaxTypeCache: JurisdictionTaxTypeCache,
    private val jurisdictionFetcher: JurisdictionFetcher
) {

    fun getAvailableTaxTypes(): List<TreeNodeWithData<UUID, TaxRecoveryType>> {
        val assignedIds = orgJurisdictionTaxTypeFetcher.getAlreadyAssignedIds()
        val taxTypeIndex = taxTypeFetcher.getAllDtos().associateBy { it.id }
        val linksByJurisdiction = jurisdictionTaxTypeCache.getActive()
            .filterNot { it.id in assignedIds }
            .groupBy { it.jurisdictionId }
        val allJurisdictions = jurisdictionFetcher.getAllDtos()
        val childJurisdictions = allJurisdictions.groupBy { it.parentJurisdictionId }

        fun buildJurisdictionNode(jurisdiction: JurisdictionDto): TreeNodeWithData<UUID, TaxRecoveryType>? {
            val taxTypeNodes = linksByJurisdiction[jurisdiction.id].orEmpty().map { link ->
                val taxType = taxTypeIndex[link.taxTypeId]
                val taxTypeLabel = "${jurisdiction.name} - ${taxType?.name}"
                TreeNodeWithData(link.id, taxTypeLabel, selectable = true, data = taxType?.taxRecoveryType)
            }
            val childNodes = childJurisdictions[jurisdiction.id].orEmpty().mapNotNull { buildJurisdictionNode(it) }
            if (taxTypeNodes.isEmpty() && childNodes.isEmpty()) return null
            return TreeNodeWithData(jurisdiction.id, jurisdiction.name, selectable = false, children = taxTypeNodes + childNodes)
        }

        return allJurisdictions
            .filter { it.parentJurisdictionId == null }
            .mapNotNull { buildJurisdictionNode(it) }
    }
}
