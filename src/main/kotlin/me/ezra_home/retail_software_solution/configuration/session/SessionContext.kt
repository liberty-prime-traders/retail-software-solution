package me.ezra_home.retail_software_solution.configuration.session

import java.util.UUID

data class SessionContext(
    var oktaId: String? = null,
    var systemUserId: UUID? = null,
    var tenantFilterIsComplete: Boolean = false,
    var organization: OrgSession? = null,
    var location: LocationSession? = null
)
