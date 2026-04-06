package me.ezra_home.retail_software_solution.organizations.business.tax_rate

import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeFetcher
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeStatus
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.api.TaxRateInsertDto
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.api.TaxRateUpdateDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeService
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxApplicationLevel
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Component
class TaxRateValidator(
    private val orgJurisdictionTaxTypeFetcher: OrgJurisdictionTaxTypeFetcher,
    private val jurisdictionTaxTypeService: JurisdictionTaxTypeService,
    private val taxRateCache: TaxRateCache
) {

    fun validateForCreate(dto: TaxRateInsertDto) {
        validateName(dto.name)
        validateDates(dto.startDate, dto.endDate)
        val parent = orgJurisdictionTaxTypeFetcher.getAllDtos()
            .firstOrNull { it.id == dto.orgJurisdictionTaxTypeId }
            ?: throw RtsGenericException("Org jurisdiction tax type not found")
        if (parent.status != OrgJurisdictionTaxTypeStatus.ACTIVE)
            throw RtsGenericException("Cannot create a rate for an inactive Tax Type")
        validateRateValue(parent.jurisdictionTaxTypeId, dto.ratePercentage, dto.rateFlatAmount)
        val taxApplicationLevel = jurisdictionTaxTypeService.getTaxApplicationLevel(parent.jurisdictionTaxTypeId)
        val overlappingRates = overlappingRates(dto.orgJurisdictionTaxTypeId, dto.startDate, dto.endDate)
        val conflictingRate = when (taxApplicationLevel) {
            TaxApplicationLevel.PRODUCT -> overlappingRates.firstOrNull { StringUtils.isEquivalent(it.name, dto.name) }
            TaxApplicationLevel.ORGANIZATION -> overlappingRates.firstOrNull()
        }
        conflictingRate?.let { throwOverlapError(it) }
    }

    fun validateForUpdate(taxRateDto: TaxRateDto, dto: TaxRateUpdateDto) {
        val id = taxRateDto.id!!
        val updatedName = dto.name?.orElseThrow { RtsGenericException("Name must not be null") }
            ?.also { validateName(it) }
        val updatedEndDate = dto.endDate?.orElseThrow { RtsGenericException("End date must not be null") }
            ?.also { validateDates(taxRateDto.startDate, it) }
        if (updatedName == null && updatedEndDate == null) return
        val effectiveName = updatedName ?: taxRateDto.name
        val effectiveEndDate = updatedEndDate ?: taxRateDto.endDate
        val taxApplicationLevel = getTaxApplicationLevel(taxRateDto.orgJurisdictionTaxTypeId)
        val candidates = overlappingRates(taxRateDto.orgJurisdictionTaxTypeId, taxRateDto.startDate, effectiveEndDate, excludeId = id)
        val conflict = when (taxApplicationLevel) {
            TaxApplicationLevel.PRODUCT -> candidates.firstOrNull { StringUtils.isEquivalent(it.name, effectiveName) }
            TaxApplicationLevel.ORGANIZATION -> candidates.firstOrNull()
        }
        conflict?.let { throwOverlapError(it) }
    }

    private fun validateName(name: String) {
        StringUtils.requireHasValue(name, "Name must not be blank")
    }

    private fun validateDates(startDate: LocalDate, endDate: LocalDate?) {
        if (endDate != null && endDate.isBefore(DateTimes.Local.Now.organization()))
            throw RtsGenericException("End date must not be in the past")
        if (endDate != null && startDate.isAfter(endDate))
            throw RtsGenericException("The start date must be before the end date")
    }

    private fun overlappingRates(
        orgJurisdictionTaxTypeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate?,
        excludeId: UUID? = null
    ): List<TaxRateDto> =
        taxRateCache.getAll().filter {
            it.orgJurisdictionTaxTypeId == orgJurisdictionTaxTypeId &&
            (excludeId == null || it.id != excludeId) &&
            (endDate == null || !it.startDate.isAfter(endDate)) &&
            (it.endDate == null || !it.endDate!!.isBefore(startDate))
        }

    private fun getTaxApplicationLevel(orgJurisdictionTaxTypeId: UUID): TaxApplicationLevel {
        val parent = orgJurisdictionTaxTypeFetcher.getAllDtos().first { it.id == orgJurisdictionTaxTypeId }
        return jurisdictionTaxTypeService.getTaxApplicationLevel(parent.jurisdictionTaxTypeId)
    }

    private fun throwOverlapError(conflict: TaxRateDto) {
        val expiry = if (conflict.endDate != null) "expires on ${conflict.endDate}" else "never expires"
        throw RtsGenericException("'${conflict.name}' starts on ${conflict.startDate} and $expiry. Please adjust your dates to avoid this overlap.")
    }

    private fun validateRateValue(
        jurisdictionTaxTypeId: UUID,
        ratePercentage: BigDecimal?,
        rateFlatAmount: BigDecimal?
    ) {
        when (jurisdictionTaxTypeService.getCalculationMethod(jurisdictionTaxTypeId)) {
            CalculationMethod.PERCENTAGE -> {
                if (ratePercentage == null)
                    throw RtsGenericException("ratePercentage is required for PERCENTAGE tax types")
                if (ratePercentage < BigDecimal.ZERO)
                    throw RtsGenericException("ratePercentage must be not be negative")
                if (ratePercentage > BigDecimal.valueOf(100))
                    throw RtsGenericException("ratePercentage must not exceed 100")
                if (rateFlatAmount != null)
                    throw RtsGenericException("rateFlatAmount must not be provided for PERCENTAGE tax types")
            }
            CalculationMethod.FLAT_PER_UNIT -> {
                if (rateFlatAmount == null)
                    throw RtsGenericException("rateFlatAmount is required for FLAT_PER_UNIT tax types")
                if (rateFlatAmount < BigDecimal.ZERO)
                    throw RtsGenericException("rateFlatAmount must not be negative")
                if (ratePercentage != null)
                    throw RtsGenericException("ratePercentage must not be provided for FLAT_PER_UNIT tax types")
            }
        }
    }

}
