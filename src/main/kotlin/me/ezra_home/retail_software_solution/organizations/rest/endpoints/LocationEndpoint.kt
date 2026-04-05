package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.location.public.LocationService
import me.ezra_home.retail_software_solution.organizations.business.location.public.LocationInsertDto
import me.ezra_home.retail_software_solution.organizations.business.location.public.LocationResponseDto
import me.ezra_home.retail_software_solution.organizations.business.location.public.LocationUpdateDto
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
@RequestMapping("secured/locations")
class LocationEndpoint(private val locationService: LocationService) {

    @GetMapping
    @PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
    //TODO: Change to @PreAuthorize("@rtsPermissions.isOrganizationMember()") when user locations are implemented
    fun getForOrganization(): Collection<LocationResponseDto> =
        locationService.getAllLocations()

    @PostMapping
    @PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
    fun createLocation(@RequestBody locationInsertDto: LocationInsertDto): LocationResponseDto =
        locationService.createLocation(locationInsertDto)

    @PutMapping
    @PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
    fun updateLocation(@RequestBody locationUpdateDto: LocationUpdateDto): LocationResponseDto =
        locationService.updateLocation(locationUpdateDto)

}
