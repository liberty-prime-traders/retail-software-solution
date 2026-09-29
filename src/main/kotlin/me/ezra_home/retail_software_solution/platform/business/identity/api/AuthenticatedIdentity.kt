package me.ezra_home.retail_software_solution.platform.business.identity.api

data class AuthenticatedIdentity(
    val provider: IdentityProvider,
    val externalId: String,
    val email: String?,
    val firstName: String,
    val lastName: String?
)
