package me.ezra_home.retail_software_solution.cross_tier.authority

import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import me.ezra_home.retail_software_solution.util.enums.RtsRole
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.util.UUID

// Batch: every user in userIds receives every role and every permission listed (cross product).
data class AuthorizationAssignmentRequest(
    val userIds: List<UUID>,
    val roles: List<RtsRole> = emptyList(),
    val permissions: List<RtsPermission> = emptyList()
) {
    init {
        if (userIds.isEmpty()) throw RtsGenericException("At least one userId must be provided")
        if (roles.isEmpty() && permissions.isEmpty()) {
            throw RtsGenericException("At least one role or permission must be provided")
        }
    }

    val allTiers: List<SchemaLevel>
        get() = roles.map { it.tier } + permissions.map { it.tier }
}
