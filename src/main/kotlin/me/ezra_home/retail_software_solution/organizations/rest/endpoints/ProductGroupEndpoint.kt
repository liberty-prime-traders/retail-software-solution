package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupService
import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupUpdateDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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

  @DeleteMapping("{id}")
  fun deletePaymentMethod(@PathVariable id: UUID): ResponseEntity<HttpStatus> {
    productGroupService.deleteProductGroup(id)
    return ResponseEntity(HttpStatus.NO_CONTENT)
  }
}
