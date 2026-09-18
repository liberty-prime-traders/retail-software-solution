package me.ezra_home.retail_software_solution.platform.rest.endpoints

import me.ezra_home.retail_software_solution.platform.business.role_assignment.api.RoleAssignmentRequest
import me.ezra_home.retail_software_solution.platform.business.role_assignment.api.RoleResolutionService
import me.ezra_home.retail_software_solution.util.enums.RtsRoleNames
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/roles")
class RoleAssignmentEndpoint(private val roleResolutionService: RoleResolutionService) {

    @PostMapping
    @PreAuthorize("hasRole('${RtsRoleNames.PLATFORM_ADMIN}')")
    fun assign(@RequestBody roleAssignmentRequest: RoleAssignmentRequest) {
        roleResolutionService.assignRole(roleAssignmentRequest.userId, roleAssignmentRequest.role)
    }

    @DeleteMapping
    @PreAuthorize("hasRole('${RtsRoleNames.PLATFORM_ADMIN}')")
    fun remove(@RequestBody roleAssignmentRequest: RoleAssignmentRequest) {
        roleResolutionService.removeRole(roleAssignmentRequest.userId, roleAssignmentRequest.role)
    }
}
