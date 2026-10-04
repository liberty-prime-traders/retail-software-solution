package me.ezra_home.retail_software_solution.locations.business.tax_entry.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.tax_entry.TaxEntryEntity
import me.ezra_home.retail_software_solution.locations.business.tax_entry.TaxEntryRepository
import org.springframework.stereotype.Service

@Service
class TaxEntryService(
    private val taxEntryRepository: TaxEntryRepository,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun findBySourceReference(sourceReferenceNumber: String, sourceType: TaxSourceType): List<TaxEntryDto> =
        taxEntryRepository.findBySourceReferenceNumberAndSourceType(sourceReferenceNumber, sourceType)
            .map { it.toDto() }

    @TransactionalOnLocationSchema
    fun createAll(dtos: List<TaxEntryCreateDto>) {
        if (dtos.isEmpty()) return
        taxEntryRepository.saveAll(dtos.map { it.toEntity() })
    }

    private fun TaxEntryEntity.toDto() = TaxEntryDto(
        sourceReferenceNumber = sourceReferenceNumber,
        sourceType = sourceType,
        direction = direction,
        taxTypeId = taxTypeId,
        taxTypeName = taxTypeName,
        jurisdictionName = jurisdictionName,
        fiscalPeriodId = fiscalPeriodId,
        calculationMethod = calculationMethod,
        rate = rate,
        taxIsBilledToCustomerSeparately = taxIsBilledToCustomerSeparately,
        taxIsIncludedInTaxableAmount = taxIsIncludedInTaxableAmount,
        taxableAmount = taxableAmount,
        resolvedTaxableBase = resolvedTaxableBase,
        taxAmount = taxAmount,
    )

    private fun TaxEntryCreateDto.toEntity() = TaxEntryEntity(
        sourceReferenceNumber = sourceReferenceNumber,
        sourceType = sourceType,
        direction = direction,
        taxTypeId = taxTypeId,
        taxTypeName = taxTypeName,
        jurisdictionName = jurisdictionName,
        fiscalPeriodId = fiscalPeriodId,
        calculationMethod = calculationMethod,
        rate = rate,
        taxIsBilledToCustomerSeparately = taxIsBilledToCustomerSeparately,
        taxIsIncludedInTaxableAmount = taxIsIncludedInTaxableAmount,
        taxableAmount = taxableAmount,
        resolvedTaxableBase = resolvedTaxableBase,
        taxAmount = taxAmount,
    )
}
