package me.ezra_home.retail_software_solution.organizations.business.feature

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class OrganizationFeatureStatusConverter : EnumConverter<OrganizationFeatureStatus>(OrganizationFeatureStatus::class.java)
