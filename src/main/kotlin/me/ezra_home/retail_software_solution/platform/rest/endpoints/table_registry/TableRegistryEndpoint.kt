package me.ezra_home.retail_software_solution.platform.rest.endpoints.table_registry

import me.ezra_home.retail_software_solution.configuration.security.RtsRoles
import me.ezra_home.retail_software_solution.platform.business.table_registry.TableRegistryService
import me.ezra_home.retail_software_solution.platform.business.table_registry.dto.TableRegistryInsertDto
import me.ezra_home.retail_software_solution.platform.business.table_registry.dto.TableRegistryResponseDto
import me.ezra_home.retail_software_solution.platform.business.table_registry.dto.TableRegistryUpdateDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("secured/table-registries")
@PreAuthorize("hasRole('${RtsRoles.ROLE_PLATFORM_ADMIN}')")
class TableRegistryEndpoint(private val tableRegistryService: TableRegistryService) {

    @GetMapping
    fun getAll(): Collection<TableRegistryResponseDto> = tableRegistryService.getAll()

    @PostMapping
    fun create(@RequestBody dto: TableRegistryInsertDto): TableRegistryResponseDto = tableRegistryService.create(dto)

    @PutMapping
    fun update(@RequestBody dto: TableRegistryUpdateDto): TableRegistryResponseDto = tableRegistryService.update(dto)

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<HttpStatus> {
        tableRegistryService.delete(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
