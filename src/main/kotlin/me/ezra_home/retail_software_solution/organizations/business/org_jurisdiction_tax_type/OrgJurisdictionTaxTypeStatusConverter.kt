package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class OrgJurisdictionTaxTypeStatusConverter : EnumConverter<OrgJurisdictionTaxTypeStatus>(OrgJurisdictionTaxTypeStatus::class.java)
