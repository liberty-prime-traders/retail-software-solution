package me.ezra_home.retail_software_solution.organizations.business.product.api

import java.io.Serializable
import java.util.UUID

data class OrganizationProductInsertDto(
    val productName: String,
    val description: String? = null,
    val productGroupId: UUID,
    val baseUnitId: UUID,
    val tagsToAdd: Set<UUID> = emptySet()
) : Serializable
