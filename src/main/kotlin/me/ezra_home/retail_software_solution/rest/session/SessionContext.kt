package me.ezra_home.retail_software_solution.rest.session

import java.util.UUID


data class SessionContext(
    var oktaId: String? = null,
    var systemUserId: UUID? = null
)
