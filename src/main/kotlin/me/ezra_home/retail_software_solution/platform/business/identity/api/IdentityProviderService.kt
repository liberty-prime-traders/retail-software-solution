package me.ezra_home.retail_software_solution.platform.business.identity.api

interface IdentityProviderService {
    fun supports(provider: IdentityProvider): Boolean
    fun authenticate(credential: String): AuthenticatedIdentity
}
