package me.ezra_home.retail_software_solution.platform.rest.endpoints

import me.ezra_home.retail_software_solution.configuration.security.RtsRoles
import me.ezra_home.retail_software_solution.organizations.business.location.public.LocationResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization.`public`.OrganizationService
import me.ezra_home.retail_software_solution.platform.business.organization.`public`.OrganizationInsertDto
import me.ezra_home.retail_software_solution.platform.business.organization.`public`.OrganizationResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization.`public`.OrganizationUpdateDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.`public`.OrganizationLaunchResponseDto
import org.springframework.http.HttpStatus
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
    fun createOrganization(@RequestBody dto: OrganizationInsertDto): OrganizationResponseDto =
        organizationService.createOrganization(dto)

    @PutMapping
    @PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
    fun updateOrganization(@RequestBody dto: OrganizationUpdateDto): OrganizationResponseDto =
        organizationService.updateOrganization(dto)

    @DeleteMapping
    @PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
    fun deleteOrganization(): ResponseEntity<HttpStatus> {
        organizationService.deleteOrganization()
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @PostMapping("launch/{domain}")
    fun launchOrganization(@PathVariable domain: String): OrganizationLaunchResponseDto =
        organizationService.attemptOrganizationLaunch(domain)

    @GetMapping("/{organizationId}/locations")
    @PreAuthorize("hasRole('${RtsRoles.ROLE_PLATFORM_ADMIN}')")
    fun getOrganizationLocations(
        @PathVariable organizationId: UUID,
    ): Collection<LocationResponseDto> {
        return organizationService.getOrganizationLocations(organizationId)
    }
}
