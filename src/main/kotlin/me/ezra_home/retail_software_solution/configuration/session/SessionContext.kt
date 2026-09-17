package me.ezra_home.retail_software_solution.configuration.session

import me.ezra_home.retail_software_solution.util.enums.RtsRole
import java.util.UUID

data class SessionContext(
    var systemUserId: UUID? = null,
    var tenantFilterIsComplete: Boolean = false,
    var organization: OrgSession? = null,
    var location: LocationSession? = null,
    var roles: Set<RtsRole> = emptySet()
)
