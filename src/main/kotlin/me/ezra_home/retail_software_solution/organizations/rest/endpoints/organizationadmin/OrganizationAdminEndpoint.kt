package me.ezra_home.retail_software_solution.organizations.rest.endpoints.organizationadmin

import me.ezra_home.retail_software_solution.organizations.business.organization_admin.OrganizationAdminResponseDto
import me.ezra_home.retail_software_solution.organizations.business.organization_admin.OrganizationAdminService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("secured/organization-admins")
@PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
class OrganizationAdminEndpoint(private val organizationAdminService: OrganizationAdminService) {

    @GetMapping
    fun getAdminHistoryForOrganization(): Collection<OrganizationAdminResponseDto> =
        organizationAdminService.getAdminHistory()

    @GetMapping("is-admin")
    @PreAuthorize("permitAll()")
    fun isOrganizationAdmin(): ResponseEntity<Boolean> {
        val isAdmin = organizationAdminService.isOrganizationAdmin()
        return ResponseEntity.ok(isAdmin)
    }

    @PostMapping("{adminId}")
    fun createOrganizationAdmin(@PathVariable adminId: UUID): OrganizationAdminResponseDto =
        organizationAdminService.createOrganizationAdmin(adminId)

    @PostMapping("terminate/{adminId}")
    fun terminateOrganizationAdmin(@PathVariable adminId: UUID): ResponseEntity<HttpStatus> {
        organizationAdminService.terminateOrganizationAdmin(adminId)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
