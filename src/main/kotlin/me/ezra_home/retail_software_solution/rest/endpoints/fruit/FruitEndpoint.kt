package me.ezra_home.retail_software_solution.controller

import me.ezra_home.retail_software_solution.model.dto.FruitRequestDTO
import me.ezra_home.retail_software_solution.model.dto.FruitResponseDTO
import me.ezra_home.retail_software_solution.service.FruitService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/fruits")
class FruitController(private val fruitService: FruitService) {

    @GetMapping("/{id}")
    fun getFruitById(@PathVariable id: UUID): ResponseEntity<FruitResponseDTO> {
        return ResponseEntity.ok(fruitService.getFruitById(id))
    }

    @GetMapping("/search")
    fun getFruitByName(@RequestParam name: String): ResponseEntity<FruitResponseDTO> {
        return ResponseEntity.ok(fruitService.getFruitByName(name))
    }

    @PostMapping
    fun createFruit(@RequestBody request: FruitRequestDTO): ResponseEntity<FruitResponseDTO> {
        return ResponseEntity.ok(fruitService.createFruit(request))
    }

    @PutMapping("/{id}")
    fun updateFruit(@PathVariable id: UUID, @RequestBody request: FruitRequestDTO): ResponseEntity<FruitResponseDTO> {
        return ResponseEntity.ok(fruitService.updateFruit(id, request))
    }

    @DeleteMapping("/{id}")
    fun deleteFruit(@PathVariable id: UUID): ResponseEntity<Unit> {
        fruitService.deleteFruit(id)
        return ResponseEntity.noContent().build()
    }
}
