package me.ezra_home.retail_software_solution.platform.rest.endpoints.jurisdiction

import me.ezra_home.retail_software_solution.platform.business.jurisdiction.JurisdictionService
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.dto.JurisdictionInsertDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.dto.JurisdictionResponseDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.dto.JurisdictionUpdateDto
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
@RequestMapping("secured/jurisdictions")
@PreAuthorize("@rtsPermissions.isPlatformAdmin()")
class JurisdictionEndpoint(
    private val jurisdictionService: JurisdictionService
) {

    @GetMapping
    @PreAuthorize("permitAll()")
    fun getAll(): Collection<JurisdictionResponseDto> = jurisdictionService.getAll()

    @PostMapping
    fun create(@RequestBody dto: JurisdictionInsertDto): JurisdictionResponseDto =
        jurisdictionService.create(dto)

    @PutMapping
    fun update(@RequestBody dto: JurisdictionUpdateDto): JurisdictionResponseDto =
        jurisdictionService.update(dto)

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<HttpStatus> {
        jurisdictionService.delete(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
