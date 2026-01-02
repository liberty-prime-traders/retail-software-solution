package me.ezra_home.retail_software_solution.organizations.rest.endpoints.product_tag

import me.ezra_home.retail_software_solution.organizations.business.product_tag.ProductTagService
import me.ezra_home.retail_software_solution.organizations.business.product_tag.dto.ProductTagRequestDto
import me.ezra_home.retail_software_solution.organizations.business.product_tag.dto.ProductTagResponseDto
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping("secured/product-tags")
class ProductTagEndpoint( private val productTagService: ProductTagService) {

    @PutMapping("{productId}")
    fun manageProductTags(
        @PathVariable productId: UUID,
        @RequestBody requestDto: ProductTagRequestDto
    ): ProductTagResponseDto {
        return productTagService.manageProductTags(productId, requestDto)
    }
}
