package me.ezra_home.retail_software_solution.platform.rest.endpoints.organizationadmin

import me.ezra_home.retail_software_solution.platform.business.organizationadmin.OrganizationAdminService
import me.ezra_home.retail_software_solution.platform.business.organizationadmin.dto.OrganizationAdminInsertDto
import me.ezra_home.retail_software_solution.platform.business.organizationadmin.dto.OrganizationAdminResponseDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("secured/organization-admins")
class OrganizationAdminEndpoint(private val organizationAdminService: OrganizationAdminService) {

    @GetMapping("{organizationId}")
    fun getAdminHistoryForOrganization(@PathVariable organizationId: UUID?): Collection<OrganizationAdminResponseDto> =
        organizationAdminService.getAdminHistoryForOrganization(organizationId)

    @PostMapping
    fun createOrganizationAdmin(@RequestBody organizationAdminInsertDto: OrganizationAdminInsertDto): OrganizationAdminResponseDto =
        organizationAdminService.createOrganizationAdmin(organizationAdminInsertDto)

    @PostMapping("terminate")
    fun terminateOrganizationAdmin(@RequestBody organizationAdminInsertDto: OrganizationAdminInsertDto): ResponseEntity<HttpStatus> {
        organizationAdminService.terminateOrganizationAdmin(organizationAdminInsertDto)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
