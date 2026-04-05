package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryService
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryUpdateDto
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
@RequestMapping("secured/product-category")
class ProductCategoryEndpoint(private val productCategoryService: ProductCategoryService) {

    @PostMapping
    fun createCategory(@RequestBody productCategoryInsertDto: ProductCategoryInsertDto): ProductCategoryResponseDto =
        productCategoryService.createCategory(productCategoryInsertDto)

    @PutMapping
    fun updateCategory(@RequestBody productCategoryDto: ProductCategoryUpdateDto): ProductCategoryResponseDto =
        productCategoryService.updateCategory(productCategoryDto)

    @GetMapping
    fun getAllCategories(): Collection<ProductCategoryResponseDto> =
        productCategoryService.getAllCategories()

    @DeleteMapping("{id}")
    fun deleteCategory(@PathVariable id: UUID): ResponseEntity<HttpStatusCode> {
        productCategoryService.deleteCategory(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
