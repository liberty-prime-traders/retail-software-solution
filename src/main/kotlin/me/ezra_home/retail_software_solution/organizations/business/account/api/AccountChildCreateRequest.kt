package me.ezra_home.retail_software_solution.organizations.business.account.api

import java.util.UUID

data class AccountChildCreateRequest(
    val parentAccountId: UUID,
    val name: String
)
