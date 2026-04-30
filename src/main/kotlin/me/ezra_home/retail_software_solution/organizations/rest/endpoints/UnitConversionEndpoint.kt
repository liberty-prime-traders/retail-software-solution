package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.unitconversion.UnitConversionDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.ConversionTargetDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionService
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionUpdateDto
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("secured/unit-conversions")
class UnitConversionEndpoint(
    private val unitConversionService: UnitConversionService,
    private val unitConversionGraphFacade: UnitConversionGraphFacade
) {

    @GetMapping
    fun getAll(): List<UnitConversionDto> = unitConversionService.getAll()

    @GetMapping("graph")
    fun getGraph(): Map<UUID, Map<UUID, ConversionTargetDto>> = unitConversionGraphFacade.getOrLoad()

    @PostMapping
    fun insert(@RequestBody dto: UnitConversionInsertDto): UnitConversionDto = unitConversionService.insert(dto)

    @PutMapping("{id}")
    fun update(@PathVariable id: UUID, @RequestBody dto: UnitConversionUpdateDto): UnitConversionDto =
        unitConversionService.update(dto.copy(id = id))

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<HttpStatusCode> {
        unitConversionService.delete(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
