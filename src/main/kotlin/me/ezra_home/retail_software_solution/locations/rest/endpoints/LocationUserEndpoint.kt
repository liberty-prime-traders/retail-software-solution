package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.cross_tier.authority.MembershipUserSummary
import me.ezra_home.retail_software_solution.cross_tier.authority.UserAccessDetail
import me.ezra_home.retail_software_solution.locations.business.location_user.api.LocationUserResponseDto
import me.ezra_home.retail_software_solution.locations.business.location_user.api.LocationUserService
import me.ezra_home.retail_software_solution.util.enums.RtsPermissionNames
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/location-users")
class LocationUserEndpoint(private val locationUserService: LocationUserService) {

    @GetMapping
    @PreAuthorize("hasAuthority('${RtsPermissionNames.VIEW_USERS_OF_LOCATION}')")
    fun getLocationUsers(): Collection<MembershipUserSummary> {
        return locationUserService.getLocationUserSummaries()
    }

    @GetMapping("access")
    @PreAuthorize("hasAuthority('${RtsPermissionNames.VIEW_USERS_OF_LOCATION}')")
    fun getUserAccess(@RequestParam userId: UUID): UserAccessDetail {
        return locationUserService.getUserAccessDetail(userId)
    }

    @PostMapping
    @PreAuthorize("hasAuthority('${RtsPermissionNames.ADD_USER_TO_LOCATION}')")
    fun createLocationUsers(@RequestBody userIds: List<UUID>): Collection<MembershipUserSummary> {
        return locationUserService.createLocationUsers(userIds)
    }

    @PostMapping("terminate")
    @PreAuthorize("hasAuthority('${RtsPermissionNames.REMOVE_USER_FROM_LOCATION}')")
    fun terminateUsers(@RequestBody locationUserIds: List<UUID>): Collection<LocationUserResponseDto> {
        return locationUserService.terminateLocationUsers(locationUserIds)
    }
}
