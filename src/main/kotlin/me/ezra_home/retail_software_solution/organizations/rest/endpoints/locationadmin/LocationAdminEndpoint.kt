package me.ezra_home.retail_software_solution.organizations.rest.endpoints.locationadmin

import me.ezra_home.retail_software_solution.organizations.business.locationadmin.LocationAdminResponseDto
import me.ezra_home.retail_software_solution.organizations.business.locationadmin.LocationAdminService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("secured/location-admins")
@PreAuthorize("@rtsPermissions.isLocationAdmin()")
class LocationAdminEndpoint(private val locationAdminService: LocationAdminService) {

    @GetMapping
    fun getAllLocationAdmins(): Collection<LocationAdminResponseDto> =
        locationAdminService.getAdminHistoryForLocation()

    @PostMapping("{adminId}")
    fun createLocationAdmin(@PathVariable("adminId") adminId: UUID): LocationAdminResponseDto =
        locationAdminService.createLocationAdmin(adminId)

    @DeleteMapping("{adminId}")
    fun terminateLocationAdmin(@PathVariable("adminId") adminId: UUID): ResponseEntity<HttpStatus> {
        locationAdminService.terminateLocationAdmin(adminId)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
