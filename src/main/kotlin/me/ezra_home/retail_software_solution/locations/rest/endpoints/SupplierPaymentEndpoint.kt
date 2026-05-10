package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentCreateDto
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentResponseDto
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentService
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentVoidCreateDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/supplier-payments")
class SupplierPaymentEndpoint(private val supplierPaymentService: SupplierPaymentService) {

    @PostMapping
    fun recordPayment(@RequestBody dto: SupplierPaymentCreateDto): SupplierPaymentResponseDto =
        supplierPaymentService.recordPayment(dto)

    @PostMapping("void")
    fun voidPayment(@RequestBody dto: SupplierPaymentVoidCreateDto): SupplierPaymentResponseDto =
        supplierPaymentService.voidPayment(dto)

    @GetMapping
    fun getByPurchaseId(@RequestParam purchaseId: UUID): List<SupplierPaymentResponseDto> =
        supplierPaymentService.getPaymentsByPurchaseId(purchaseId)
}
