package me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.OrgJurisdictionTaxTypeService
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.JurisdictionCache
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.dto.JurisdictionDto
import me.ezra_home.retail_software_solution.platform.business.tax_type.CalculationMethod
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxApplicationLevel
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxTypeCache
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.ui_models.TreeNode
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class JurisdictionTaxTypeService(
    private val jurisdictionTaxTypeMapper: JurisdictionTaxTypeMapper,
    private val jurisdictionTaxTypeCache: JurisdictionTaxTypeCache,
    private val jurisdictionCache: JurisdictionCache,
    private val taxTypeCache: TaxTypeCache,
    private val orgJurisdictionTaxTypeService: OrgJurisdictionTaxTypeService
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getCalculationMethod(jurisdictionTaxTypeId: UUID): CalculationMethod {
        val link = jurisdictionTaxTypeCache.getAll().find { it.id == jurisdictionTaxTypeId }
            ?: throw RtsGenericException("Jurisdiction tax type not found")
        return taxTypeCache.getAll().find { it.id == link.taxTypeId }?.calculationMethod
            ?: throw RtsGenericException("Tax type not found for jurisdiction link")
    }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getTaxApplicationLevel(jurisdictionTaxTypeId: UUID): TaxApplicationLevel {
        val link = jurisdictionTaxTypeCache.getAll().find { it.id == jurisdictionTaxTypeId }
            ?: throw RtsGenericException("Jurisdiction tax type not found")
        return taxTypeCache.getAll().find { it.id == link.taxTypeId }?.taxApplicationLevel
            ?: throw RtsGenericException("Tax type not found for jurisdiction link")
    }

    fun createAll(dtos: List<JurisdictionTaxTypeInsertDto>) {
        if (dtos.isEmpty()) return
        val taxTypeIds = taxTypeCache.getAll().mapTo(HashSet()) { it.id }
        val jurisdictionIds = jurisdictionCache.getAll().mapTo(HashSet()) { it.id }
        val existingLinks = jurisdictionTaxTypeCache.getAll().mapTo(HashSet()) { it.taxTypeId to it.jurisdictionId }
        dtos.forEach { dto ->
            if (dto.taxTypeId !in taxTypeIds) throw RtsGenericException("Tax type not found")
            if (dto.jurisdictionId !in jurisdictionIds) throw RtsGenericException("Jurisdiction not found")
            if ((dto.taxTypeId to dto.jurisdictionId) in existingLinks)
                throw RtsGenericException("A link between this tax type and jurisdiction already exists")
        }
        jurisdictionTaxTypeCache.upsertAll(dtos.map { jurisdictionTaxTypeMapper.toDomainDto(it) })
    }

    fun addOrReactivate(jurisdictionId: UUID, taxTypeIds: List<UUID>) {
        if (taxTypeIds.isEmpty()) return
        val taxTypeIdSet = taxTypeCache.getAll().mapTo(HashSet()) { it.id }
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
                !existing.active -> toReactivate.add(existing.also { it.active = true })
            }
        }
        if (toReactivate.isNotEmpty()) {
            jurisdictionTaxTypeCache.upsertAll(toReactivate)
        }
        if (toCreate.isNotEmpty()) {
            jurisdictionTaxTypeCache.upsertAll(toCreate.map { jurisdictionTaxTypeMapper.toDomainDto(it) })
        }
    }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAvailableTaxTypes(): List<TreeNode<UUID>> {
        val assignedIds = orgJurisdictionTaxTypeService.getActivelyAssignedJurisdictionTaxTypeIds()
        val taxTypeIndex = taxTypeCache.getAll().associateBy { it.id }
        val linksByJurisdiction = jurisdictionTaxTypeCache.getActive()
            .filterNot { it.id in assignedIds }
            .groupBy { it.jurisdictionId }
        val childJurisdictions = jurisdictionCache.getAll().groupBy { it.parentJurisdictionId }

        fun buildJurisdictionNode(jurisdiction: JurisdictionDto): TreeNode<UUID>? {
            val taxTypeNodes = linksByJurisdiction[jurisdiction.id].orEmpty().map { link ->
                val taxTypeLabel = "${jurisdiction.name} - ${taxTypeIndex[link.taxTypeId]?.name}"
                TreeNode(link.getNullSafeId(), taxTypeLabel, selectable = true)
            }
            val childNodes = childJurisdictions[jurisdiction.id].orEmpty().mapNotNull { buildJurisdictionNode(it) }
            if (taxTypeNodes.isEmpty() && childNodes.isEmpty()) return null
            return TreeNode(jurisdiction.getNullSafeId(), jurisdiction.name, selectable = false, children = taxTypeNodes + childNodes)
        }

        return jurisdictionCache.getAll()
            .filter { it.parentJurisdictionId == null }
            .mapNotNull { buildJurisdictionNode(it) }
    }

    fun stopByTaxTypeIds(jurisdictionId: UUID, taxTypeIds: List<UUID>) {
        val linkIndex = jurisdictionTaxTypeCache.getAll()
            .filter { it.jurisdictionId == jurisdictionId }
            .associateBy { it.taxTypeId }
        val dtos = taxTypeIds.map { taxTypeId ->
            linkIndex[taxTypeId]?.also { it.active = false }
                ?: throw RtsGenericException("Tax type $taxTypeId is not linked to this jurisdiction")
        }
        jurisdictionTaxTypeCache.upsertAll(dtos)
    }
}
