package me.ezra_home.retail_software_solution.platform.business.auth.api

interface IdentityProviderService {
    fun supports(provider: IdentityProvider): Boolean
    fun authenticate(credential: String): AuthenticatedIdentity
}
