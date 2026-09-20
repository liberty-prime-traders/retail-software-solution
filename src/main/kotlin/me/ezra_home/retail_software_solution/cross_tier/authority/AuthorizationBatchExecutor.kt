package me.ezra_home.retail_software_solution.cross_tier.authority

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import me.ezra_home.retail_software_solution.util.enums.RtsRole
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AuthorizationBatchExecutor(
    private val roleGrantService: RoleGrantService,
    private val permissionGrantService: PermissionGrantService
) {

    @TransactionalOnPlatformSchema
    fun assignAtPlatformTier(request: AuthorizationAssignmentRequest) =
        apply(request, roleGrantService::assignRoles, permissionGrantService::assignPermissions)

    @TransactionalOnOrganizationSchema
    fun assignAtOrganizationTier(request: AuthorizationAssignmentRequest) =
        apply(request, roleGrantService::assignRoles, permissionGrantService::assignPermissions)

    @TransactionalOnLocationSchema
    fun assignAtLocationTier(request: AuthorizationAssignmentRequest) =
        apply(request, roleGrantService::assignRoles, permissionGrantService::assignPermissions)

    @TransactionalOnPlatformSchema
    fun removeAtPlatformTier(request: AuthorizationAssignmentRequest) =
        apply(request, roleGrantService::removeRoles, permissionGrantService::removePermissions)

    @TransactionalOnOrganizationSchema
    fun removeAtOrganizationTier(request: AuthorizationAssignmentRequest) =
        apply(request, roleGrantService::removeRoles, permissionGrantService::removePermissions)

    @TransactionalOnLocationSchema
    fun removeAtLocationTier(request: AuthorizationAssignmentRequest) =
        apply(request, roleGrantService::removeRoles, permissionGrantService::removePermissions)

    private fun apply(
        request: AuthorizationAssignmentRequest,
        roleAction: (Collection<UUID>, Collection<RtsRole>) -> Unit,
        permissionAction: (Collection<UUID>, Collection<RtsPermission>) -> Unit
    ) {
        roleAction(request.userIds, request.roles)
        permissionAction(request.userIds, request.permissions)
    }
}
