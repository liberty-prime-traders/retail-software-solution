package me.ezra_home.retail_software_solution.platform.rest.endpoints

import me.ezra_home.retail_software_solution.platform.business.organization_join_request.api.OrganizationAdminJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.api.OrganizationJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.api.OrganizationJoinRequestService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("secured/join-requests")
class OrganizationJoinRequestEndpoint(private val organizationJoinRequestService: OrganizationJoinRequestService) {

    @GetMapping("me")
    fun getUserJoinRequests(): Collection<OrganizationJoinRequestResponseDto> =
        organizationJoinRequestService.getUserJoinRequests()

    @GetMapping
    @PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
    fun getJoinRequests(): Collection<OrganizationAdminJoinRequestResponseDto> =
        organizationJoinRequestService.getOrganizationJoinRequests()

    @PostMapping("admit")
    @PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
    fun admitUsers(@RequestBody joinRequestIds: Collection<UUID>): Collection<OrganizationAdminJoinRequestResponseDto> {
        return organizationJoinRequestService.admitUsers(joinRequestIds)
    }

    @PostMapping("deny")
    @PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
    fun denyUsers(@RequestBody joinRequestIds: Collection<UUID>): Collection<OrganizationAdminJoinRequestResponseDto> {
        return organizationJoinRequestService.denyUsers(joinRequestIds)
    }
}
