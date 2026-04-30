package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.org_profile.api.OrgProfileService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
@RequestMapping("secured/organization-profile")
@PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
class OrganizationProfileEndpoint(private val orgProfileService: OrgProfileService) {

    @PostMapping("seed-defaults")
    fun seedDefaults(): ResponseEntity<HttpStatus> {
        orgProfileService.applySeedDefaults()
        return ResponseEntity(HttpStatus.OK)
    }
}
