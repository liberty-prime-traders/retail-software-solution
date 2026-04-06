package me.ezra_home.retail_software_solution.organizations.business.organization_user.api

import java.io.Serializable
import java.util.UUID

data class OrganizationUserInsertDto(
    val userId: UUID,
    val joinRequestId: UUID? = null
) : Serializable
