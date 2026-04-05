package me.ezra_home.retail_software_solution.platform.business.reserved_subdomain

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.platform.business.reserved_subdomain.`public`.ReservedDomainStatus
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class ReservedDomainStatusConverter : EnumConverter<ReservedDomainStatus>(ReservedDomainStatus::class.java)
