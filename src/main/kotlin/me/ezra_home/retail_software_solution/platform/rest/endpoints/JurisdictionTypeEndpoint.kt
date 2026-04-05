package me.ezra_home.retail_software_solution.platform.rest.endpoints

import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.`public`.JurisdictionTypeService
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.`public`.JurisdictionTypeInsertDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.`public`.JurisdictionTypeResponseDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.`public`.JurisdictionTypeUpdateDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/jurisdiction-types")
@PreAuthorize("@rtsPermissions.isPlatformAdmin()")
class JurisdictionTypeEndpoint(private val jurisdictionTypeService: JurisdictionTypeService) {

    @GetMapping
    fun getAll(): Collection<JurisdictionTypeResponseDto> = jurisdictionTypeService.getAll()

    @PostMapping
    fun create(@RequestBody dto: JurisdictionTypeInsertDto): JurisdictionTypeResponseDto =
        jurisdictionTypeService.create(dto)

    @PutMapping
    fun update(@RequestBody dto: JurisdictionTypeUpdateDto): JurisdictionTypeResponseDto =
        jurisdictionTypeService.update(dto)

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<HttpStatus> {
        jurisdictionTypeService.delete(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
