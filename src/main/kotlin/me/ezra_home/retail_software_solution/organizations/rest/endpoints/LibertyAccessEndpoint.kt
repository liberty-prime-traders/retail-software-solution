package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.liberty_access.api.LibertyAccessService
import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/liberty-permissions")
class LibertyAccessEndpoint(private val libertyAccessService: LibertyAccessService) {

    @GetMapping
    @PreAuthorize("permitAll()")
    fun getLibertyPermissionsOfCurrentUser(): List<RtsPermission> =
        libertyAccessService.getLibertyPermissionsOfCurrentUser()
}
