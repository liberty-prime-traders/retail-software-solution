package me.ezra_home.retail_software_solution.configuration.security

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.platform.business.auth.api.IdentityProvider
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class IdentityProviderConverter : EnumConverter<IdentityProvider>(IdentityProvider::class.java)
