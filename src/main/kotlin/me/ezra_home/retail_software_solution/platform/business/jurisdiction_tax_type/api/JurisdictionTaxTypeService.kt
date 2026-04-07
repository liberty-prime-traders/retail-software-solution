package me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeFetcher
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.api.JurisdictionDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.api.JurisdictionFetcher
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.JurisdictionTaxTypeCache
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.PlatformTaxTypeDto
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxApplicationLevel
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxTypeFetcher
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.ui_models.TreeNode
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class JurisdictionTaxTypeService(
    private val jurisdictionTaxTypeCache: JurisdictionTaxTypeCache,
    private val jurisdictionFetcher: JurisdictionFetcher,
    private val taxTypeFetcher: TaxTypeFetcher,
    private val orgJurisdictionTaxTypeFetcher: OrgJurisdictionTaxTypeFetcher
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getCalculationMethod(jurisdictionTaxTypeId: UUID): CalculationMethod {
        val link = jurisdictionTaxTypeCache.getAll().find { it.id == jurisdictionTaxTypeId }
            ?: throw RtsGenericException("Jurisdiction tax type not found")
        return taxTypeFetcher.getAllDtos().find { it.id == link.taxTypeId }?.calculationMethod
            ?: throw RtsGenericException("Tax type not found for jurisdiction link")
    }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getTaxApplicationLevel(jurisdictionTaxTypeId: UUID): TaxApplicationLevel {
        val link = jurisdictionTaxTypeCache.getAll().find { it.id == jurisdictionTaxTypeId }
            ?: throw RtsGenericException("Jurisdiction tax type not found")
        return taxTypeFetcher.getAllDtos().find { it.id == link.taxTypeId }?.taxApplicationLevel
            ?: throw RtsGenericException("Tax type not found for jurisdiction link")
    }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun isInUse(taxTypeId: UUID): Boolean =
        jurisdictionTaxTypeCache.getAll().any { it.taxTypeId == taxTypeId }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getActiveIds(): Set<UUID> =
        jurisdictionTaxTypeCache.getActive().map { it.id }.toHashSet()

    @TransactionalOnPlatformSchema(readOnly = true)
    fun buildIndex(): Map<UUID, PlatformTaxTypeDto> {
        val jurisdictionIndex = jurisdictionFetcher.getAllDtos().associateBy { it.id }
        val taxTypeIndex = taxTypeFetcher.getAllDtos().associateBy { it.id }
        return jurisdictionTaxTypeCache.getAll().mapNotNull { link ->
            val jurisdiction = jurisdictionIndex[link.jurisdictionId] ?: return@mapNotNull null
            val taxType = taxTypeIndex[link.taxTypeId] ?: return@mapNotNull null
            link.id to PlatformTaxTypeDto(taxType.id, "${jurisdiction.name} - ${taxType.name}")
        }.toMap()
    }

    fun createAll(dtos: List<JurisdictionTaxTypeInsertDto>) {
        if (dtos.isEmpty()) return
        val taxTypeIds = taxTypeFetcher.getAllDtos().mapTo(HashSet()) { it.id }
        val jurisdictionIds = jurisdictionFetcher.getAllDtos().mapTo(HashSet()) { it.id }
        val existingLinks = jurisdictionTaxTypeCache.getAll().mapTo(HashSet()) { it.taxTypeId to it.jurisdictionId }
        dtos.forEach { dto ->
            if (dto.taxTypeId !in taxTypeIds) throw RtsGenericException("Tax type not found")
            if (dto.jurisdictionId !in jurisdictionIds) throw RtsGenericException("Jurisdiction not found")
            if ((dto.taxTypeId to dto.jurisdictionId) in existingLinks)
                throw RtsGenericException("A link between this tax type and jurisdiction already exists")
        }
        jurisdictionTaxTypeCache.createAll(dtos)
    }

    fun addOrReactivate(jurisdictionId: UUID, taxTypeIds: List<UUID>) {
        if (taxTypeIds.isEmpty()) return
        val taxTypeIdSet = taxTypeFetcher.getAllDtos().mapTo(HashSet()) { it.id }
        val existingJurisdictionTaxTypes = jurisdictionTaxTypeCache.getAll()
            .filter { it.jurisdictionId == jurisdictionId }
            .associateBy { it.taxTypeId }
        val toReactivate = mutableListOf<JurisdictionTaxTypeDto>()
        val toCreate = mutableListOf<JurisdictionTaxTypeInsertDto>()
        taxTypeIds.forEach { taxTypeId ->
            if (taxTypeId !in taxTypeIdSet) throw RtsGenericException("Tax type not found")
            val existing = existingJurisdictionTaxTypes[taxTypeId]
            when {
                existing == null -> toCreate.add(JurisdictionTaxTypeInsertDto(taxTypeId, jurisdictionId))
                !existing.active -> toReactivate.add(existing.copy(active = true))
            }
        }
        if (toReactivate.isNotEmpty()) {
            jurisdictionTaxTypeCache.saveAll(toReactivate)
        }
        if (toCreate.isNotEmpty()) {
            jurisdictionTaxTypeCache.createAll(toCreate)
        }
    }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAvailableTaxTypes(): List<TreeNode<UUID>> {
        val assignedIds = orgJurisdictionTaxTypeFetcher.getAlreadyAssignedIds()
        val taxTypeIndex = taxTypeFetcher.getAllDtos().associateBy { it.id }
        val linksByJurisdiction = jurisdictionTaxTypeCache.getActive()
            .filterNot { it.id in assignedIds }
            .groupBy { it.jurisdictionId }
        val allJurisdictions = jurisdictionFetcher.getAllDtos()
        val childJurisdictions = allJurisdictions.groupBy { it.parentJurisdictionId }

        fun buildJurisdictionNode(jurisdiction: JurisdictionDto): TreeNode<UUID>? {
            val taxTypeNodes = linksByJurisdiction[jurisdiction.id].orEmpty().map { link ->
                val taxTypeLabel = "${jurisdiction.name} - ${taxTypeIndex[link.taxTypeId]?.name}"
                TreeNode(link.id, taxTypeLabel, selectable = true)
            }
            val childNodes = childJurisdictions[jurisdiction.id].orEmpty().mapNotNull { buildJurisdictionNode(it) }
            if (taxTypeNodes.isEmpty() && childNodes.isEmpty()) return null
            return TreeNode(jurisdiction.id, jurisdiction.name, selectable = false, children = taxTypeNodes + childNodes)
        }

        return allJurisdictions
            .filter { it.parentJurisdictionId == null }
            .mapNotNull { buildJurisdictionNode(it) }
    }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getActiveTaxTypeIdsByJurisdiction(): Map<UUID, List<UUID>> =
        jurisdictionTaxTypeCache.getAll()
            .filter { it.active }
            .groupBy { it.jurisdictionId }
            .mapValues { (_, dtos) -> dtos.map { it.taxTypeId } }

    fun stopByTaxTypeIds(jurisdictionId: UUID, taxTypeIds: List<UUID>) {
        val linkIndex = jurisdictionTaxTypeCache.getAll()
            .filter { it.jurisdictionId == jurisdictionId }
            .associateBy { it.taxTypeId }
        val dtos = taxTypeIds.map { taxTypeId ->
            linkIndex[taxTypeId]?.copy(active = false)
                ?: throw RtsGenericException("Tax type $taxTypeId is not linked to this jurisdiction")
        }
        jurisdictionTaxTypeCache.saveAll(dtos)
    }
}
