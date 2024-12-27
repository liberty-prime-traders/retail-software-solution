package me.ezra_home.retail_software_solution.rest.endpoints.organization

import me.ezra_home.retail_software_solution.business.organization.OrganizationService
import me.ezra_home.retail_software_solution.business.organization.dto.OrganizationInsertDto
import me.ezra_home.retail_software_solution.business.organization.dto.OrganizationResponseDto
import me.ezra_home.retail_software_solution.business.organization.dto.OrganizationUpdateDto
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
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
    fun getAllOrganizations(): Collection<OrganizationResponseDto> = organizationService.getAllOrganizations()

    @PostMapping
    fun updateOrganization(@RequestBody organizationInsertDto: OrganizationInsertDto): OrganizationResponseDto =
        organizationService.createOrganization(organizationInsertDto)

    @PutMapping
    fun updateOrganization(@RequestBody organizationUpdateDto: OrganizationUpdateDto): OrganizationResponseDto =
        organizationService.updateOrganization(organizationUpdateDto)

    @DeleteMapping("{id}")
    fun deleteOrganization(@PathVariable id: UUID?): ResponseEntity<HttpStatusCode> {
        organizationService.deleteOrganization(id)
        return ResponseEntity(HttpStatusCode.valueOf(HttpStatus.NO_CONTENT.value()))
    }
}
