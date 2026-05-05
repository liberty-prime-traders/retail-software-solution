package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentService
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentVoidCreateDto
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/sale-payments")
class SalePaymentEndpoint(
    private val salePaymentService: SalePaymentService,
) {

    @PostMapping
    fun recordPayment(@RequestBody dto: SalePaymentCreateDto): SalePaymentResponseDto =
        salePaymentService.recordPayment(dto)

    @PostMapping("void")
    fun voidPayment(@RequestBody dto: SalePaymentVoidCreateDto): SalePaymentResponseDto {
        return salePaymentService.voidPayment(dto)
    }
}
