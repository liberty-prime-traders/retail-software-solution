package me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.api.JurisdictionFetcher
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.JurisdictionTaxTypeCache
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.PlatformTaxTypeDto
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxApplicationLevel
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxTypeDto
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxTypeFetcher
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema(readOnly = true)
class JurisdictionTaxTypeFetcher(
    private val jurisdictionTaxTypeCache: JurisdictionTaxTypeCache,
    private val jurisdictionFetcher: JurisdictionFetcher,
    private val taxTypeFetcher: TaxTypeFetcher
) {

    fun getCalculationMethod(jurisdictionTaxTypeId: UUID): CalculationMethod {
        return getTaxType(jurisdictionTaxTypeId).calculationMethod
    }

    fun getTaxApplicationLevel(jurisdictionTaxTypeId: UUID): TaxApplicationLevel {
        return getTaxType(jurisdictionTaxTypeId).taxApplicationLevel
    }

    fun getTaxType(jurisdictionTaxTypeId: UUID): TaxTypeDto {
        val link = getLink(jurisdictionTaxTypeId)
        return taxTypeFetcher.getAllDtos().find { it.id == link.taxTypeId }
            ?: throw RtsGenericException("Tax type not found for jurisdiction link")
    }

    private fun getLink(jurisdictionTaxTypeId: UUID): JurisdictionTaxTypeDto {
        return jurisdictionTaxTypeCache.getAll().find { it.id == jurisdictionTaxTypeId }
            ?: throw RtsGenericException("Jurisdiction tax type not found for link id: $jurisdictionTaxTypeId")
    }

    fun isInUse(taxTypeId: UUID): Boolean =
        jurisdictionTaxTypeCache.getAll().any { it.taxTypeId == taxTypeId }

    fun getActiveIds(): Set<UUID> =
        jurisdictionTaxTypeCache.getActive().map { it.id }.toHashSet()

    fun buildIndex(): Map<UUID, PlatformTaxTypeDto> {
        val jurisdictionIndex = jurisdictionFetcher.getAllDtos().associateBy { it.id }
        val taxTypeIndex = taxTypeFetcher.getAllDtos().associateBy { it.id }
        return jurisdictionTaxTypeCache.getAll().mapNotNull { link ->
            val jurisdiction = jurisdictionIndex[link.jurisdictionId] ?: return@mapNotNull null
            val taxType = taxTypeIndex[link.taxTypeId] ?: return@mapNotNull null
            link.id to PlatformTaxTypeDto(taxType.id, "${jurisdiction.name} - ${taxType.name}", taxType.taxRecoveryType)
        }.toMap()
    }

    fun getActiveTaxTypeIdsByJurisdiction(): Map<UUID, List<UUID>> =
        jurisdictionTaxTypeCache.getAll()
            .filter { it.active }
            .groupBy { it.jurisdictionId }
            .mapValues { (_, dtos) -> dtos.map { it.taxTypeId } }
}
