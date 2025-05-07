package me.ezra_home.retail_software_solution.organizations.rest.endpoints.location

import me.ezra_home.retail_software_solution.organizations.business.location.LocationService
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationResponseDto
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationUpdateDto
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
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
    fun getForOrganization(): Collection<LocationResponseDto> =
        locationService.getLocationsForOrganization()

    @PostMapping
    @PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
    fun createLocation(@RequestBody locationInsertDto: LocationInsertDto): LocationResponseDto =
        locationService.createLocation(locationInsertDto)

    @PutMapping
    @PreAuthorize("@rtsPermissions.isLocationAdmin()")
    fun updateLocation(@RequestBody locationUpdateDto: LocationUpdateDto): LocationResponseDto =
        locationService.updateLocation(locationUpdateDto)

    @DeleteMapping
    @PreAuthorize("@rtsPermissions.isLocationAdmin()")
    fun deleteLocation(): ResponseEntity<HttpStatusCode> {
        locationService.deleteLocation()
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
