package me.ezra_home.retail_software_solution.platform.rest.endpoints

import me.ezra_home.retail_software_solution.cross_tier.authority.Authority
import me.ezra_home.retail_software_solution.cross_tier.authority.AuthorityHolderResponse
import me.ezra_home.retail_software_solution.cross_tier.authority.AuthorityType
import me.ezra_home.retail_software_solution.cross_tier.authority.AuthorizationCatalogService
import me.ezra_home.retail_software_solution.cross_tier.authority.PermissionResponse
import me.ezra_home.retail_software_solution.platform.business.authority.api.PlatformAuthorityHolderService
import me.ezra_home.retail_software_solution.util.enums.RtsPermissionNames
import me.ezra_home.retail_software_solution.util.enums.RtsRole
import me.ezra_home.retail_software_solution.util.enums.RtsRoleNames
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/authorities")
class AuthorityCatalogEndpoint(
    private val authorizationCatalogService: AuthorizationCatalogService,
    private val platformAuthorityHolderService: PlatformAuthorityHolderService
) {

    @GetMapping("per-role")
    fun getPermissionsForRole(@RequestParam role: RtsRole): List<PermissionResponse> {
        return authorizationCatalogService.getPermissionsForRole(role)
    }

    @GetMapping("platform")
    @PreAuthorize("hasRole('${RtsRoleNames.PLATFORM_ADMIN}')")
    fun getPlatformCatalog(): List<Authority> = authorizationCatalogService.getCatalogForTier(SchemaLevel.PLATFORM)

    @GetMapping("platform/holders")
    @PreAuthorize("hasRole('${RtsRoleNames.PLATFORM_ADMIN}')")
    fun getPlatformAuthorityHolders(
        @RequestParam authorityName: String,
        @RequestParam authorityType: AuthorityType
    ): List<AuthorityHolderResponse> =
        platformAuthorityHolderService.getHolders(authorityName, authorityType)

    @GetMapping("organization")
    @PreAuthorize("hasAuthority('${RtsPermissionNames.MANAGE_ORG_ACCESS}')")
    fun getOrgCatalog(): List<Authority> = authorizationCatalogService.getCatalogForTier(SchemaLevel.ORGANIZATION)

    @GetMapping("location")
    @PreAuthorize("hasAuthority('${RtsPermissionNames.MANAGE_LOCATION_ACCESS}')")
    fun getLocationCatalog(): List<Authority> = authorizationCatalogService.getCatalogForTier(SchemaLevel.LOCATION)
}
