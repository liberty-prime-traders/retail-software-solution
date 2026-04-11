package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.feature.api.OrganizationFeatureResponseDto
import me.ezra_home.retail_software_solution.organizations.business.feature.api.OrganizationFeatureService
import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/org-features")
class OrganizationFeatureEndpoint(private val orgFeatureService: OrganizationFeatureService) {

    @GetMapping
    fun getAll(): List<OrganizationFeatureResponseDto> = orgFeatureService.getAll()

    @PostMapping("activate")
    fun activate(@RequestBody features: HashSet<Feature>): List<OrganizationFeatureResponseDto> =
        orgFeatureService.activate(features)

    @PostMapping("deactivate")
    fun deactivate(@RequestBody features: HashSet<Feature>): List<OrganizationFeatureResponseDto> =
        orgFeatureService.deactivate(features)
}
