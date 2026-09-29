package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.TaxAccountsValidator
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.OrgJurisdictionTaxTypeCache
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeFetcher
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.PlatformTaxTypeDto
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxRecoveryType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class OrgJurisdictionTaxTypeService(
    private val orgJurisdictionTaxTypeCache: OrgJurisdictionTaxTypeCache,
    private val jurisdictionTaxTypeFetcher: JurisdictionTaxTypeFetcher,
    private val taxAccountsValidator: TaxAccountsValidator,
    private val accountService: AccountService
) {

    fun getAll(): List<OrgJurisdictionTaxTypeResponseDto> {
        val index = jurisdictionTaxTypeFetcher.buildIndex()
        val accountNamesByCode = accountService.getAccountNamesByCode()
        return orgJurisdictionTaxTypeCache.getAll().map { toResponseDto(it, index, accountNamesByCode) }
    }

    fun createAll(dtos: List<OrgJurisdictionTaxTypeInsertDto>): List<OrgJurisdictionTaxTypeResponseDto> {
        val index = jurisdictionTaxTypeFetcher.buildIndex()
        val activeIds = jurisdictionTaxTypeFetcher.getActiveIds()
        val existingJurisdictionIds = orgJurisdictionTaxTypeCache.getAll().mapTo(HashSet()) { it.jurisdictionTaxTypeId }
        val seen = HashSet<UUID>()
        dtos.forEach { dto ->
            val platformTaxType = index[dto.jurisdictionTaxTypeId] ?: throw RtsGenericException("Jurisdiction tax type not found: ${dto.jurisdictionTaxTypeId}")
            val taxLabel = platformTaxType.label
            if (!seen.add(dto.jurisdictionTaxTypeId))
                throw RtsGenericException("Duplicate jurisdiction tax type in request: $taxLabel")
            if (dto.jurisdictionTaxTypeId !in activeIds)
                throw RtsGenericException("Jurisdiction tax type not found or is already stopped: $taxLabel")
            if (dto.jurisdictionTaxTypeId in existingJurisdictionIds)
                throw RtsGenericException("$taxLabel is already registered for this organization")
            taxAccountsValidator.validate(dto.payableAccountCode, dto.recoverableAccountCode, platformTaxType)
        }
        val savedDtos = orgJurisdictionTaxTypeCache.createAll(dtos)
        val accountNamesByCode = accountService.getAccountNamesByCode()
        return savedDtos.map { toResponseDto(it, index, accountNamesByCode) }
    }

    fun update(dto: OrgJurisdictionTaxTypeUpdateDto): OrgJurisdictionTaxTypeResponseDto {
        val existing = orgJurisdictionTaxTypeCache.getAll().find { it.id == dto.id }
            ?: throw UpdatingNonExistingRecordException()
        val index = jurisdictionTaxTypeFetcher.buildIndex()
        val platformTaxType = index[existing.jurisdictionTaxTypeId] ?: throw RtsGenericException("Jurisdiction tax type not found")
        val updated = dto.applyTo(existing).let {
            if (platformTaxType.taxRecoveryType != TaxRecoveryType.RECOVERABLE) {
                it.copy(recoverableAccountCode = null)
            } else it
        }
        taxAccountsValidator.validate(updated.payableAccountCode, updated.recoverableAccountCode, platformTaxType)
        orgJurisdictionTaxTypeCache.saveAll(listOf(updated))
        val accountNamesByCode = accountService.getAccountNamesByCode()
        return toResponseDto(updated, index, accountNamesByCode)
    }

    private fun toResponseDto(
        dto: OrgJurisdictionTaxTypeDto,
        taxTypeById: Map<UUID, PlatformTaxTypeDto>,
        accountNamesByCode: Map<String, String>
    ): OrgJurisdictionTaxTypeResponseDto {
        val platformTaxType = taxTypeById[dto.jurisdictionTaxTypeId] ?: throw RtsGenericException("Jurisdiction tax type not found")
        return OrgJurisdictionTaxTypeResponseDto(
            id = dto.id,
            referenceNumber = dto.referenceNumber,
            createdOn = dto.createdOn,
            status = dto.status,
            platformTaxId = platformTaxType.id,
            taxLabel = platformTaxType.label,
            taxRecoveryType = platformTaxType.taxRecoveryType,
            payableAccountCode = dto.payableAccountCode,
            payableAccount = accountNamesByCode[dto.payableAccountCode],
            recoverableAccountCode = dto.recoverableAccountCode,
            recoverableAccount = accountNamesByCode[dto.recoverableAccountCode],
            taxInclusive = dto.taxInclusive
        )
    }
}
