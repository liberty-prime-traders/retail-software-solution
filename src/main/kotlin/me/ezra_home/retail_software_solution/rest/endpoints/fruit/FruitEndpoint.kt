package me.ezra_home.retail_software_solution.rest.endpoints.fruit

import me.ezra_home.retail_software_solution.business.fruit.FruitService
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitInsertDto
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitResponseDto
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitUpdateDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("secured/fruit")
class FruitEndpoint(private val fruitService: FruitService) {

    @PostMapping
    fun createFruit(@RequestBody fruitInsertDto: FruitInsertDto): FruitResponseDto =
        fruitService.createFruit(fruitInsertDto)

    @PutMapping
    fun updateFruit(@RequestBody fruitUpdateDto: FruitUpdateDto): FruitResponseDto =
        fruitService.updateFruit(fruitUpdateDto)

    @GetMapping
    fun getAllFruits(): Collection<FruitResponseDto> =
        fruitService.getAllFruits()

    @DeleteMapping("{id}")
    fun deleteFruit(@PathVariable id: UUID): ResponseEntity<Unit> {
        fruitService.deleteFruit(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
