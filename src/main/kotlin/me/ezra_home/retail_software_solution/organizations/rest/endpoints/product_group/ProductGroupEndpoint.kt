package me.ezra_home.retail_software_solution.organizations.rest.endpoints.product_group

import me.ezra_home.retail_software_solution.organizations.business.product_group.ProductGroupService
import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupUpdateDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/product-groups")
class ProductGroupEndpoint(
  private val productGroupService: ProductGroupService
) {

  @PostMapping
  fun createProductGroup(@RequestBody productGroupInsertDto: ProductGroupInsertDto): ProductGroupResponseDto =
    productGroupService.createProductGroup(productGroupInsertDto)

  @PutMapping
  fun updateProductGroup(@RequestBody productGroupDto: ProductGroupUpdateDto): ProductGroupResponseDto =
    productGroupService.updateProductGroup(productGroupDto)

  @GetMapping
  fun getAllProductGroups(): Collection<ProductGroupResponseDto> =
    productGroupService.getAllProductGroups()
}
