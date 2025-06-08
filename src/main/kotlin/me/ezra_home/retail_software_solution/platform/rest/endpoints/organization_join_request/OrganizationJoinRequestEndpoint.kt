package me.ezra_home.retail_software_solution.platform.rest.endpoints.organization_join_request

import me.ezra_home.retail_software_solution.platform.business.organization_join_request.OrganizationJoinRequestService
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationAdminJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationJoinRequestResponseDto
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
}
