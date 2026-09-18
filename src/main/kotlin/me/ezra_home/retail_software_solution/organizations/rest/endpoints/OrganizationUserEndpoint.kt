package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.organization_user.api.OrganizationUserResponseDto
import me.ezra_home.retail_software_solution.organizations.business.organization_user.api.OrganizationUserService
import me.ezra_home.retail_software_solution.util.enums.RtsRoleNames
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/organization-users")
class OrganizationUserEndpoint(private val organizationUserService: OrganizationUserService) {
    @GetMapping
    @PreAuthorize("hasRole('${RtsRoleNames.VIEW_USERS_OF_ORG}')")
    fun getOrganizationUsers(): Collection<OrganizationUserResponseDto> {
        return organizationUserService.getOrganizationUsers()
    }

    @PostMapping("terminate")
    @PreAuthorize("hasRole('${RtsRoleNames.REMOVE_USER_FROM_ORG}')")
    fun terminateUsers(@RequestBody organizationUserIds: List<UUID>): Collection<OrganizationUserResponseDto> {
        return organizationUserService.terminateOrganizationUsers(organizationUserIds)
    }
}
