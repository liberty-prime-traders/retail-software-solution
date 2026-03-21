package me.ezra_home.retail_software_solution.platform.business.reserved_subdomain

import java.io.Serializable
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.platform.model.ReservedSubdomainEntity}
 */
data class ReservedSubdomainDto(
    val id: UUID?,
    val subdomain: String?,
    val createdBy: String?,
    val status: ReservedDomainStatus?,
    val referenceNumber: String?
): Serializable
