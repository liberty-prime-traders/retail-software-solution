package me.ezra_home.retail_software_solution.platform.rest.endpoints.organization

import me.ezra_home.retail_software_solution.configuration.security.RtsRoles
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationService
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationInsertDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationUpdateDto
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
    fun createOrganization(@RequestBody organizationInsertDto: OrganizationInsertDto): OrganizationResponseDto =
        organizationService.createOrganization(organizationInsertDto)

    @PutMapping
    fun updateOrganization(@RequestBody organizationUpdateDto: OrganizationUpdateDto): OrganizationResponseDto =
        organizationService.updateOrganization(organizationUpdateDto)

    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('${RtsRoles.ROLE_PLATFORM_ADMIN}')")
    fun deleteOrganization(@PathVariable id: UUID?): ResponseEntity<HttpStatus> {
        organizationService.deleteOrganization(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
