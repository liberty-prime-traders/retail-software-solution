package me.ezra_home.retail_software_solution.platform.business.role_assignment.api

import me.ezra_home.retail_software_solution.util.enums.RtsRole
import java.util.UUID

data class RoleAssignmentRequest(
    val userId: UUID,
    val role: RtsRole
)
