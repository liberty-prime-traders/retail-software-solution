package me.ezra_home.retail_software_solution.organizations.rest.endpoints.unitgroup

import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupService
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.dto.UnitGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.dto.UnitGroupResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.dto.UnitGroupUpdateDto
import org.springframework.http.HttpStatus
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
@RequestMapping("secured/unitgroups")
class UnitGroupEndpoint(private val unitGroupService: UnitGroupService) {

    @GetMapping
    fun getAllUnitGroups(): Collection<UnitGroupResponseDto> = unitGroupService.getAllUnitGroups()

    @PostMapping
    fun updateUnitGroup(@RequestBody unitGroupInsertDto: UnitGroupInsertDto): UnitGroupResponseDto =
        unitGroupService.createUnitGroup(unitGroupInsertDto)

    @PutMapping
    fun updateUnitGroup(@RequestBody unitGroupUpdateDto: UnitGroupUpdateDto): UnitGroupResponseDto =
        unitGroupService.updateUnitGroup(unitGroupUpdateDto)

    @DeleteMapping("{id}")
    fun deleteUnitGroup(@PathVariable id: UUID?): ResponseEntity<HttpStatus> {
        unitGroupService.deleteUnitGroup(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
