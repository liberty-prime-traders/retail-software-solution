package me.ezra_home.retail_software_solution.platform.rest.endpoints.table_registry

import me.ezra_home.retail_software_solution.configuration.security.RtsRoles
import me.ezra_home.retail_software_solution.platform.business.table_registry.TableRegistryService
import me.ezra_home.retail_software_solution.platform.business.table_registry.dto.TableRegistryResponseDto
import me.ezra_home.retail_software_solution.platform.business.table_registry.dto.TableRegistryUpdateDto
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/table-registries")
@PreAuthorize("hasRole('${RtsRoles.ROLE_PLATFORM_ADMIN}')")
class TableRegistryEndpoint(private val tableRegistryService: TableRegistryService) {

    @GetMapping
    fun getAll(): Collection<TableRegistryResponseDto> = tableRegistryService.getAll()

    @PutMapping("validate/{id}")
    fun validate(@PathVariable id: UUID): TableRegistryResponseDto = tableRegistryService.validate(id)

    @PutMapping
    fun update(@RequestBody dto: TableRegistryUpdateDto): TableRegistryResponseDto = tableRegistryService.update(dto)

}
