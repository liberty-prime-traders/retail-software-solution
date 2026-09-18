package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.location_user.api.LocationUserInsertDto
import me.ezra_home.retail_software_solution.locations.business.location_user.api.LocationUserResponseDto
import me.ezra_home.retail_software_solution.locations.business.location_user.api.LocationUserService
import me.ezra_home.retail_software_solution.util.enums.RtsRoleNames
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/location-users")
class LocationUserEndpoint(private val locationUserService: LocationUserService) {

    @GetMapping
    @PreAuthorize("hasRole('${RtsRoleNames.VIEW_USERS_OF_LOCATION}')")
    fun getLocationUsers(): Collection<LocationUserResponseDto> {
        return locationUserService.getLocationUsers()
    }

    @PostMapping
    @PreAuthorize("hasRole('${RtsRoleNames.ADD_USER_TO_LOCATION}')")
    fun createLocationUser(@RequestBody locationUserInsertDto: LocationUserInsertDto): LocationUserResponseDto {
        return locationUserService.createLocationUser(locationUserInsertDto)
    }

    @PostMapping("terminate")
    @PreAuthorize("hasRole('${RtsRoleNames.REMOVE_USER_FROM_LOCATION}')")
    fun terminateUsers(@RequestBody locationUserIds: List<UUID>): Collection<LocationUserResponseDto> {
        return locationUserService.terminateLocationUsers(locationUserIds)
    }
}
