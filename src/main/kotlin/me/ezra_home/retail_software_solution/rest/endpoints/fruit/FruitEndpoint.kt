package me.ezra_home.retail_software_solution.rest.endpoints.fruit

import me.ezra_home.retail_software_solution.business.fruit.FruitService
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitUpdateDTO
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitInsertDTO
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitResponseDTO
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import java.util.UUID
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
@RequestMapping("secured/fruits")
class FruitEndpoint(private val fruitService: FruitService) {

    @GetMapping
    fun getAllFruits(): Collection<FruitResponseDTO> = fruitService.getAllFruits()

    @PostMapping
    fun createFruit(@RequestBody fruitInsertDto: FruitInsertDTO): FruitResponseDTO =
        fruitService.createFruit(fruitInsertDto)

    @PutMapping
    fun updateFruit(@RequestBody fruitUpdateDto: FruitUpdateDTO): FruitResponseDTO =
        fruitService.updateFruit(fruitUpdateDto)

    @DeleteMapping("{id}")
    fun deleteFruit(@PathVariable id: UUID?): ResponseEntity<HttpStatusCode> {
        fruitService.deleteFruit(id)
        return ResponseEntity(HttpStatusCode.valueOf(HttpStatus.NO_CONTENT.value()))
    }
}
