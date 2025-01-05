package me.ezra_home.retail_software_solution.rest.endpoints.location

import me.ezra_home.retail_software_solution.business.location.LocationService
import me.ezra_home.retail_software_solution.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.business.location.dto.LocationResponseDto
import me.ezra_home.retail_software_solution.business.location.dto.LocationUpdateDto
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("secured/locations")
class LocationEndpoint(private val locationService: LocationService) {

    @GetMapping
    fun getForOrganization(@RequestParam("organizationId") organizationId: UUID): Collection<LocationResponseDto> =
        locationService.getLocationsForOrganization(organizationId)

    @PostMapping
    fun createLocation(@RequestBody locationInsertDto: LocationInsertDto): LocationResponseDto =
        locationService.createLocation(locationInsertDto)

    @PutMapping
    fun updateLocation(@RequestBody locationUpdateDto: LocationUpdateDto): LocationResponseDto =
        locationService.updateLocation(locationUpdateDto)

    @DeleteMapping("{id}")
    fun deleteLocation(@PathVariable id: UUID?): ResponseEntity<HttpStatusCode> {
        locationService.deleteLocation(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
