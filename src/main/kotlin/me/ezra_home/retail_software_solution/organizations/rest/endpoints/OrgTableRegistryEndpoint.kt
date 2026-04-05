package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.api.OrgTableRegistryService
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.api.OrgTableRegistryResponseDto
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.api.OrgTableRegistryUpdateDto
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/org-table-registries")
@PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
class OrgTableRegistryEndpoint(private val orgTableRegistryService: OrgTableRegistryService) {

    @GetMapping
    fun getAll(): Collection<OrgTableRegistryResponseDto> = orgTableRegistryService.getAll()

    @PutMapping
    fun update(@RequestBody dto: OrgTableRegistryUpdateDto): OrgTableRegistryResponseDto = orgTableRegistryService.update(dto)
}
