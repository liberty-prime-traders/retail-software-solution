package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.OrgJurisdictionTaxTypeCache
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.OrgJurisdictionTaxTypeMapper
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeService
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.PlatformTaxTypeDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class OrgJurisdictionTaxTypeService(
    private val orgJurisdictionTaxTypeMapper: OrgJurisdictionTaxTypeMapper,
    private val orgJurisdictionTaxTypeCache: OrgJurisdictionTaxTypeCache,
    private val jurisdictionTaxTypeService: JurisdictionTaxTypeService
) {

    fun getAll(): List<OrgJurisdictionTaxTypeResponseDto> {
        val index = jurisdictionTaxTypeService.buildIndex()
        return orgJurisdictionTaxTypeCache.getAll().map { toResponseDto(it, index) }
    }

    fun createAll(dtos: List<OrgJurisdictionTaxTypeInsertDto>): List<OrgJurisdictionTaxTypeResponseDto> {
        val index = jurisdictionTaxTypeService.buildIndex()
        val activeIds = jurisdictionTaxTypeService.getActiveIds()
        val existingJurisdictionIds = orgJurisdictionTaxTypeCache.getAll().mapTo(HashSet()) { it.jurisdictionTaxTypeId }
        val seen = HashSet<UUID>()
        dtos.forEach { dto ->
            val label = index[dto.jurisdictionTaxTypeId]?.label ?: dto.jurisdictionTaxTypeId.toString()
            if (!seen.add(dto.jurisdictionTaxTypeId))
                throw RtsGenericException("Duplicate jurisdiction tax type in request: $label")
            if (dto.jurisdictionTaxTypeId !in activeIds)
                throw RtsGenericException("Jurisdiction tax type not found or stopped: $label")
            if (dto.jurisdictionTaxTypeId in existingJurisdictionIds)
                throw RtsGenericException("An assignment already exists for: $label")
        }
        val domainDtos = dtos.map { orgJurisdictionTaxTypeMapper.toDomainDto(it) }
        orgJurisdictionTaxTypeCache.saveAll(domainDtos)
        return domainDtos.map { toResponseDto(it, index) }
    }

    fun update(dto: OrgJurisdictionTaxTypeUpdateDto): OrgJurisdictionTaxTypeResponseDto {
        val existing = orgJurisdictionTaxTypeCache.getAll().find { it.id == dto.id }
            ?: throw UpdatingNonExistingRecordException()
        existing.status = dto.status
        orgJurisdictionTaxTypeCache.saveAll(listOf(existing))
        return toResponseDto(existing, jurisdictionTaxTypeService.buildIndex())
    }

    private fun toResponseDto(dto: OrgJurisdictionTaxTypeDto, index: Map<UUID, PlatformTaxTypeDto>): OrgJurisdictionTaxTypeResponseDto {
        val platformTaxType = index[dto.jurisdictionTaxTypeId]
            ?: throw RtsGenericException("Jurisdiction tax type not found: ${dto.jurisdictionTaxTypeId}")
        return orgJurisdictionTaxTypeMapper.toResponseDto(dto, platformTaxType)
    }
}
