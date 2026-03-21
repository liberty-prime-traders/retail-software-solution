package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.dto.OrgJurisdictionTaxTypeResponseDto
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.dto.OrgJurisdictionTaxTypeUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.dto.OrgJurisdictionTaxTypeInsertDto
import me.ezra_home.retail_software_solution.organizations.model.OrgJurisdictionTaxTypeEntity
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.JurisdictionTaxTypeCache
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.JurisdictionTaxTypeLabeler
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class OrgJurisdictionTaxTypeService(
    private val orgJurisdictionTaxTypeMapper: OrgJurisdictionTaxTypeMapper,
    private val orgJurisdictionTaxTypeRepository: OrgJurisdictionTaxTypeRepository,
    private val orgJurisdictionTaxTypeCache: OrgJurisdictionTaxTypeCache,
    private val jurisdictionTaxTypeCache: JurisdictionTaxTypeCache,
    private val labeler: JurisdictionTaxTypeLabeler
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAssignedJurisdictionTaxTypeIds(): Set<UUID> {
        return orgJurisdictionTaxTypeCache.getAll()
            .filter { it.endDate == null }
            .mapTo(HashSet()) { it.jurisdictionTaxTypeId }
    }

    fun getAll(): List<OrgJurisdictionTaxTypeResponseDto> {
        val labelIndex = labeler.buildLabelIndex()
        return orgJurisdictionTaxTypeCache.getAll().map { toResponseDto(it, labelIndex) }
    }

    fun createAll(dtos: List<OrgJurisdictionTaxTypeInsertDto>): List<OrgJurisdictionTaxTypeResponseDto> {
        val labelIndex = labeler.buildLabelIndex()
        val activeIds = jurisdictionTaxTypeCache.getActive().mapNotNull { it.id }.toHashSet()
        val openIds = getAssignedJurisdictionTaxTypeIds()
        dtos.forEach { dto ->
            if (dto.jurisdictionTaxTypeId !in activeIds)
                throw RtsGenericException("Jurisdiction tax type not found or stopped: ${dto.jurisdictionTaxTypeId}")
            if (dto.jurisdictionTaxTypeId in openIds)
                throw RtsGenericException("An open assignment already exists for: ${dto.jurisdictionTaxTypeId}")
        }
        val entities = dtos.map { orgJurisdictionTaxTypeMapper.toEntity(it) }
        orgJurisdictionTaxTypeCache.saveAll(entities)
        return entities.map { toResponseDto(it, labelIndex) }
    }

    fun update(dto: OrgJurisdictionTaxTypeUpdateDto): OrgJurisdictionTaxTypeResponseDto {
        val entity = orgJurisdictionTaxTypeRepository.findById(dto.id)
            .orElseThrow { UpdatingNonExistingRecordException() }
        if (!dto.endDate.isAfter(entity.startDate))
            throw RtsGenericException("End date must be after start date")
        entity.endDate = dto.endDate
        orgJurisdictionTaxTypeCache.saveAll(listOf(entity))
        return toResponseDto(entity, labeler.buildLabelIndex())
    }

    private fun toResponseDto(entity: OrgJurisdictionTaxTypeEntity, labelIndex: Map<UUID, String>): OrgJurisdictionTaxTypeResponseDto {
        val label = labelIndex[entity.jurisdictionTaxTypeId]
            ?: throw RtsGenericException("Jurisdiction tax type not found: ${entity.jurisdictionTaxTypeId}")
        return orgJurisdictionTaxTypeMapper.toResponseDto(entity, label)
    }
}
