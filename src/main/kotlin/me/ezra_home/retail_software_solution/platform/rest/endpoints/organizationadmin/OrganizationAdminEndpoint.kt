package me.ezra_home.retail_software_solution.platform.rest.endpoints.organizationadmin

import me.ezra_home.retail_software_solution.platform.business.organizationadmin.OrganizationAdminResponseDto
import me.ezra_home.retail_software_solution.platform.business.organizationadmin.OrganizationAdminService
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
class OrganizationAdminEndpoint(private val organizationAdminService: OrganizationAdminService) {

    @GetMapping
    @PreAuthorize("rtsPermissions.operateOnOrganization()")
    fun getAdminHistoryForOrganization(): Collection<OrganizationAdminResponseDto> =
        organizationAdminService.getAdminHistoryForOrganization()

    @PostMapping("{adminId}")
    @PreAuthorize("rtsPermissions.operateOnOrganization()")
    fun createOrganizationAdmin(@PathVariable("adminId") adminId: UUID): OrganizationAdminResponseDto =
        organizationAdminService.createOrganizationAdmin(adminId)

    @PostMapping("terminate/{adminId}")
    @PreAuthorize("rtsPermissions.operateOnOrganization()")
    fun terminateOrganizationAdmin(@PathVariable("adminId") adminId: UUID): ResponseEntity<HttpStatus> {
        organizationAdminService.terminateOrganizationAdmin(adminId)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
