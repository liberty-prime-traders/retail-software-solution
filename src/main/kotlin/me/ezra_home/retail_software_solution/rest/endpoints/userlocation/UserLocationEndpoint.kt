package me.ezra_home.retail_software_solution.rest.endpoints.userlocation

import me.ezra_home.retail_software_solution.business.userlocation.UserLocationService
import me.ezra_home.retail_software_solution.business.userlocation.dto.UserLocationRequestDto
import me.ezra_home.retail_software_solution.business.userlocation.dto.UserLocationResponseDto
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
@RequestMapping("secured/user-location")
class UserLocationEndpoint(private val userLocationService: UserLocationService) {

    @GetMapping
    fun getAll(): Collection<UserLocationResponseDto> = userLocationService.getAllUserLocationAssignments()

    @PostMapping
    fun updateLocationAssignments(@RequestBody userLocationRequestDto: UserLocationRequestDto): UserLocationResponseDto =
        userLocationService.updateLocationAssignments(userLocationRequestDto)
}
