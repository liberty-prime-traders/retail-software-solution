package me.ezra_home.retail_software_solution.platform.rest.endpoints.organization

import me.ezra_home.retail_software_solution.configuration.security.RtsRoles
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationService
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationUpsertDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationAdminJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationLaunchResponseDto
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("secured/organizations")
class OrganizationEndpoint(private val organizationService: OrganizationService) {

    @GetMapping
    @PreAuthorize("hasRole('${RtsRoles.ROLE_PLATFORM_ADMIN}')")
    fun getAllOrganizations(): Collection<OrganizationResponseDto> = organizationService.getAllOrganizations()

    @PostMapping
    @PreAuthorize("hasRole('${RtsRoles.ROLE_CREATE_ORGANIZATION}')")
    fun createOrganization(@RequestBody organizationInsertDto: OrganizationUpsertDto): OrganizationResponseDto =
        organizationService.createOrganization(organizationInsertDto)

    @PutMapping
    @PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
    fun updateOrganization(@RequestBody organizationUpdateDto: OrganizationUpsertDto): OrganizationResponseDto =
        organizationService.updateOrganization(organizationUpdateDto)

    @DeleteMapping
    @PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
    fun deleteOrganization(): ResponseEntity<HttpStatus> {
        organizationService.deleteOrganization()
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @PostMapping("/launch/{domain}")
    fun launchOrganization(@PathVariable domain: String): OrganizationLaunchResponseDto =
        organizationService.attemptOrganizationLaunch(domain)

    @GetMapping("/me/join-requests")
    fun getUserJoinRequests(): Collection<OrganizationJoinRequestResponseDto> =
        organizationService.getUserJoinRequests()

    @GetMapping("/join-requests")
    @PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
    fun getJoinRequests(): Collection<OrganizationAdminJoinRequestResponseDto> =
        organizationService.getOrganizationJoinRequests()

    @GetMapping("/admit/{joinRequestId}")
    @PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
    fun admitUser(@PathVariable joinRequestId: UUID): ResponseEntity<HttpStatusCode> {
        return ResponseEntity(HttpStatus.OK)
    }

}
