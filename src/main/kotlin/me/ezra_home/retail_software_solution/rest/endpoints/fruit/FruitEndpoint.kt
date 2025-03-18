package me.ezra_home.retail_software_solution.controller

import me.ezra_home.retail_software_solution.model.dto.FruitRequestDTO
import me.ezra_home.retail_software_solution.model.dto.FruitResponseDTO
import me.ezra_home.retail_software_solution.service.FruitService
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

@RestController
@RequestMapping("secured/fruits")
class FruitController(private val fruitService: FruitService) {

    @GetMapping("{id}")
    fun getFruitById(@PathVariable id: UUID): FruitResponseDTO {
        return fruitService.getFruitById(id)
    }

    @GetMapping("search")
    fun getFruitByName(@RequestParam name: String): FruitResponseDTO {
        return fruitService.getFruitByName(name)
    }

    @PostMapping
    fun createFruit(@RequestBody request: FruitRequestDTO): FruitResponseDTO {
        return fruitService.createFruit(request)
    }

    @PutMapping("{id}")
    fun updateFruit(@PathVariable id: UUID, @RequestBody request: FruitRequestDTO): FruitResponseDTO {
        return fruitService.updateFruit(id, request)
    }

    @DeleteMapping("{id}")
    fun deleteFruit(@PathVariable id: UUID): ResponseEntity<Unit> {
        fruitService.deleteFruit(id)
        return ResponseEntity.noContent().build()
    }
}
