package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.OrgJurisdictionTaxTypeCache
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class OrgJurisdictionTaxTypeFetcher(private val orgJurisdictionTaxTypeCache: OrgJurisdictionTaxTypeCache) {

    fun getAllDtos(): Collection<OrgJurisdictionTaxTypeDto> = orgJurisdictionTaxTypeCache.getAll()

    fun getAlreadyAssignedIds(): Set<UUID> =
        orgJurisdictionTaxTypeCache.getAll().mapTo(HashSet()) { it.jurisdictionTaxTypeId }
}
