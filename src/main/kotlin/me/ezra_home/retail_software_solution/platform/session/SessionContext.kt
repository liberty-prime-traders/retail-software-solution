package me.ezra_home.retail_software_solution.platform.session

import java.util.UUID


data class SessionContext(
    var oktaId: String? = null,
    var systemUserId: UUID? = null,
    var locationSchemaName: String? = null,
    var tenantFilterIsComplete: Boolean = false,
    var organizationId: UUID? = null,
    var locationId: UUID? = null
)
