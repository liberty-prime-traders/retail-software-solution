package me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.JurisdictionCache
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxTypeCache
import me.ezra_home.retail_software_solution.platform.model.JurisdictionTaxTypeEntity
import me.ezra_home.retail_software_solution.util.enums.CalculationMethod
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class JurisdictionTaxTypeService(
    private val jurisdictionTaxTypeMapper: JurisdictionTaxTypeMapper,
    private val jurisdictionTaxTypeCache: JurisdictionTaxTypeCache,
    private val taxTypeCache: TaxTypeCache,
    private val jurisdictionCache: JurisdictionCache
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun jurisdictionTaxTypeExists(id: UUID): Boolean =
        jurisdictionTaxTypeCache.getAll().any { it.id == id }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getCalculationMethod(jurisdictionTaxTypeId: UUID): CalculationMethod {
        val link = jurisdictionTaxTypeCache.getAll().find { it.id == jurisdictionTaxTypeId }
            ?: throw RtsGenericException("Jurisdiction tax type not found")
        return taxTypeCache.getAll().find { it.id == link.taxTypeId }?.calculationMethod
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
        jurisdictionTaxTypeCache.upsertAll(dtos.map { jurisdictionTaxTypeMapper.toEntity(it) })
    }

    fun addOrReactivate(jurisdictionId: UUID, taxTypeIds: List<UUID>) {
        if (taxTypeIds.isEmpty()) return
        val taxTypeIdSet = taxTypeCache.getAll().mapTo(HashSet()) { it.id }
        val existingJurisdictionTaxTypes = jurisdictionTaxTypeCache.getAll()
            .filter { it.jurisdictionId == jurisdictionId }
            .associateBy { it.taxTypeId }
        val toReactivate = mutableListOf<JurisdictionTaxTypeEntity>()
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
            jurisdictionTaxTypeCache.upsertAll(toCreate.map { jurisdictionTaxTypeMapper.toEntity(it) })
        }
    }

    fun stopByTaxTypeIds(jurisdictionId: UUID, taxTypeIds: List<UUID>) {
        val linkIndex = jurisdictionTaxTypeCache.getAll()
            .filter { it.jurisdictionId == jurisdictionId }
            .associateBy { it.taxTypeId }
        val entities = taxTypeIds.map { taxTypeId ->
            linkIndex[taxTypeId]?.also { it.active = false }
                ?: throw RtsGenericException("Tax type $taxTypeId is not linked to this jurisdiction")
        }
        jurisdictionTaxTypeCache.upsertAll(entities)
    }
}
