package me.ezra_home.retail_software_solution.organizations.rest.endpoints.product

import me.ezra_home.retail_software_solution.organizations.business.product.ProductService
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductUpdateDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/products")
class ProductEndpoint(private val productService: ProductService) {

    @PostMapping
    fun createProduct(@RequestBody productInsertDto: ProductInsertDto): ProductResponseDto =
        productService.createProduct(productInsertDto)

    @PutMapping
    fun updateProduct(@RequestBody productDto: ProductUpdateDto): ProductResponseDto =
        productService.updateProduct(productDto)

    @PutMapping("{productId}/deactivate")
    fun deactivateProduct(@PathVariable productId: UUID): ProductResponseDto =
        productService.deactivateProduct(productId)

    @PutMapping("{productId}/reactivate")
    fun reactivateProduct(@PathVariable productId: UUID): ProductResponseDto =
        productService.reactivateProduct(productId)

    @GetMapping
    fun getAllProducts(): Collection<ProductResponseDto> =
        productService.getTopProducts()
}
