package me.ezra_home.retail_software_solution.platform.business.identity.api

data class LoginRequest(
    val provider: IdentityProvider,
    val credential: String,
    val rolesToVerify: List<String> = emptyList()
)
