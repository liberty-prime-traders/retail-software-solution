package me.ezra_home.retail_software_solution.organizations.rest.endpoints.organization_prefix_configuration

import me.ezra_home.retail_software_solution.business.prefix_configuration.shared.dto.PrefixConfigurationResponseDto
import me.ezra_home.retail_software_solution.business.prefix_configuration.shared.dto.PrefixConfigurationUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.prefix_configuration.OrganizationPrefixConfigurationService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("secured/prefix-configurations/organizations")
@PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
class OrganizationPrefixConfigurationEndpoint(private val organizationPrefixConfigurationService: OrganizationPrefixConfigurationService) {

    @GetMapping
    fun getForTableRegistry(
        @RequestParam tableRegistryId: UUID
    ): Collection<PrefixConfigurationResponseDto> =
        organizationPrefixConfigurationService.getForTableRegistry(tableRegistryId)

    @PatchMapping
    fun updatePrefix(
        @RequestBody dto: PrefixConfigurationUpdateDto
    ): PrefixConfigurationResponseDto =
        organizationPrefixConfigurationService.updatePrefix(dto)
}
