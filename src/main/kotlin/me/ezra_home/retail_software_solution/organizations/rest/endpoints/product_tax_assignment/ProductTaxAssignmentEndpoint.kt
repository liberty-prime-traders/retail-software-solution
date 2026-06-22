package me.ezra_home.retail_software_solution.organizations.rest.endpoints.product_tax_assignment

import me.ezra_home.retail_software_solution.organizations.business.product_tax_assignment.ProductTaxAssignmentService
import me.ezra_home.retail_software_solution.organizations.business.product_tax_assignment.dto.CloseProductTaxAssignmentDto
import me.ezra_home.retail_software_solution.organizations.business.product_tax_assignment.dto.ProductTaxAssignmentInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_tax_assignment.dto.ProductTaxAssignmentResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product_tax_assignment.dto.UpdatePendingProductTaxAssignmentDto
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
@RequestMapping("secured/product-tax-assignments")
class ProductTaxAssignmentEndpoint(private val productTaxAssignmentService: ProductTaxAssignmentService) {

    @GetMapping("product/{productId}")
    fun getByProduct(@PathVariable productId: UUID): Collection<ProductTaxAssignmentResponseDto> =
        productTaxAssignmentService.getByProduct(productId)

    @PostMapping
    fun assign(@RequestBody dto: ProductTaxAssignmentInsertDto): ProductTaxAssignmentResponseDto =
        productTaxAssignmentService.assign(dto)

    @PutMapping("{id}/close")
    fun close(
        @PathVariable id: UUID,
        @RequestBody dto: CloseProductTaxAssignmentDto
    ): ProductTaxAssignmentResponseDto = productTaxAssignmentService.close(id, dto.effectiveTo)

    @PutMapping("{id}/pending")
    fun updatePending(
        @PathVariable id: UUID,
        @RequestBody dto: UpdatePendingProductTaxAssignmentDto
    ): ProductTaxAssignmentResponseDto =
        productTaxAssignmentService.updatePending(id, dto.effectiveFrom, dto.effectiveTo)

    @DeleteMapping("{id}")
    fun deletePending(@PathVariable id: UUID): ResponseEntity<HttpStatus> {
        productTaxAssignmentService.deletePending(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
