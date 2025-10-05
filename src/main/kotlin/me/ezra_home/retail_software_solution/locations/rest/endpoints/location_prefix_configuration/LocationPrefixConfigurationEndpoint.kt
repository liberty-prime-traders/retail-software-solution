package me.ezra_home.retail_software_solution.locations.rest.endpoints.location_prefix_configuration

import me.ezra_home.retail_software_solution.business.prefix_configuration.shared.dto.PrefixConfigurationResponseDto
import me.ezra_home.retail_software_solution.business.prefix_configuration.shared.dto.PrefixConfigurationUpdateDto
import me.ezra_home.retail_software_solution.locations.business.prefix_configuration.LocationPrefixConfigurationService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("secured/prefix-configurations/locations")
@PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
class LocationPrefixConfigurationEndpoint(private val locationPrefixConfigurationService: LocationPrefixConfigurationService) {

    @GetMapping
    fun getForTableRegistry(
        @RequestParam tableRegistryId: UUID
    ): Collection<PrefixConfigurationResponseDto> =
        locationPrefixConfigurationService.getForTableRegistry(tableRegistryId)

    @PatchMapping
    fun updatePrefix(
        @RequestBody dto: PrefixConfigurationUpdateDto
    ): PrefixConfigurationResponseDto =
        locationPrefixConfigurationService.updatePrefix(dto)
}
