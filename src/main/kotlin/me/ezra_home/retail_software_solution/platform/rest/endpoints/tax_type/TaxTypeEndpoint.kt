package me.ezra_home.retail_software_solution.platform.rest.endpoints.tax_type

import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxTypeService
import me.ezra_home.retail_software_solution.platform.business.tax_type.dto.TaxTypeInsertDto
import me.ezra_home.retail_software_solution.platform.business.tax_type.dto.TaxTypeResponseDto
import me.ezra_home.retail_software_solution.platform.business.tax_type.dto.TaxTypeUpdateDto
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
@RequestMapping("secured/tax-types")
@PreAuthorize("@rtsPermissions.isPlatformAdmin()")
class TaxTypeEndpoint(private val taxTypeService: TaxTypeService) {

    @GetMapping
    fun getAll(): Collection<TaxTypeResponseDto> = taxTypeService.getAll()

    @PostMapping
    fun create(@RequestBody dto: TaxTypeInsertDto): TaxTypeResponseDto =
        taxTypeService.create(dto)

    @PutMapping
    fun update(@RequestBody dto: TaxTypeUpdateDto): TaxTypeResponseDto =
        taxTypeService.update(dto)

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<HttpStatus> {
        taxTypeService.delete(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
