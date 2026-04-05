package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.unitvalue.public.UnitValueService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.public.UnitValueInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.public.UnitValueResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.public.UnitValueUpdateDto
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("secured/unitvalues")
class UnitValueEndpoint(private val unitValueService: UnitValueService) {

    @GetMapping
    fun getForUnitGroup(@RequestParam("unitGroupId") unitGroupId: UUID?): Collection<UnitValueResponseDto> {
        return if (unitGroupId == null) {
            unitValueService.getAllUnitValues()
        } else {
            unitValueService.getUnitValuesForUnitGroup(unitGroupId)
        }
    }

    @PostMapping
    fun createUnitValue(@RequestBody unitValueInsertDto: UnitValueInsertDto): UnitValueResponseDto =
        unitValueService.createUnitValue(unitValueInsertDto)

    @PutMapping
    fun updateUnitValue(@RequestBody unitValueUpdateDto: UnitValueUpdateDto): UnitValueResponseDto =
        unitValueService.updateUnitValue(unitValueUpdateDto)

    @DeleteMapping("{id}")
    fun deleteUnitValue(@PathVariable id: UUID?): ResponseEntity<HttpStatusCode> {
        unitValueService.deleteUnitValue(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
