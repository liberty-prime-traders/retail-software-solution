package me.ezra_home.retail_software_solution.rest.endpoints.locations.category

import me.ezra_home.retail_software_solution.locations.business.category.CategoryService
import me.ezra_home.retail_software_solution.locations.business.category.dto.CategoryInsertDto
import me.ezra_home.retail_software_solution.locations.business.category.dto.CategoryResponseDto
import me.ezra_home.retail_software_solution.locations.business.category.dto.CategoryUpdateDto
import java.util.UUID
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
@RequestMapping("secured/category")
class CategoryEndpoint(private val categoryService: CategoryService) {

    @PostMapping
    fun createCategory(@RequestBody categoryInsertDto: CategoryInsertDto): CategoryResponseDto =
        categoryService.createCategory(categoryInsertDto)

    @PutMapping
    fun updateCategory(@RequestBody categoryDto: CategoryUpdateDto): CategoryResponseDto =
        categoryService.updateCategory(categoryDto)

    @GetMapping
    fun getAllCategories(): Collection<CategoryResponseDto> =
        categoryService.getAllCategories()

    @DeleteMapping("{id}")
    fun deleteCategory(@PathVariable id: UUID?): ResponseEntity<HttpStatusCode> {
        categoryService.deleteCategory(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
