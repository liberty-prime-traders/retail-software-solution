package me.ezra_home.retail_software_solution.rest.endpoints.category

import me.ezra_home.retail_software_solution.business.category.CategoryService
import me.ezra_home.retail_software_solution.business.category.dto.CategoryInsertDto
import me.ezra_home.retail_software_solution.business.category.dto.CategoryResponseDto
import me.ezra_home.retail_software_solution.business.category.dto.CategoryUpdateDto
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
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
}