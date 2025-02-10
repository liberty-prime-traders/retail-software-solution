package me.ezra_home.retail_software_solution.rest.endpoints.unit

import java.util.UUID
import me.ezra_home.retail_software_solution.business.unit.UnitService
import me.ezra_home.retail_software_solution.business.unit.dto.UnitInsertDto
import me.ezra_home.retail_software_solution.business.unit.dto.UnitResponseDto
import me.ezra_home.retail_software_solution.business.unit.dto.UnitUpdateDto
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

@CrossOrigin
@RestController
@RequestMapping("secured/unit")
class UnitEndpoint(private val unitService: UnitService) {

    @PostMapping
    fun createUnit(@RequestBody unitInsertDto: UnitInsertDto): UnitResponseDto =
        unitService.createUnit(unitInsertDto)

    @PutMapping
    fun updateUnit(@RequestBody unitDto: UnitUpdateDto): UnitResponseDto =
        unitService.updateUnit(unitDto)

    @GetMapping
    fun getAllUnits(): Collection<UnitResponseDto> =
        unitService.getAllUnits()

    @DeleteMapping("{id}")
    fun deleteUnit(@PathVariable id: UUID?): ResponseEntity<HttpStatusCode> {
        unitService.deleteUnit(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}