package me.ezra_home.retail_software_solution.rest.endpoints.payment_method

import me.ezra_home.retail_software_solution.business.payment_method.PaymentMethodService
import me.ezra_home.retail_software_solution.business.payment_method.dto.PaymentMethodInsertDto
import me.ezra_home.retail_software_solution.business.payment_method.dto.PaymentMethodResponseDto
import me.ezra_home.retail_software_solution.business.payment_method.dto.PaymentMethodUpdateDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("secured/payment-methods")
class PaymentMethodEndpoint (
    private val paymentMethodService: PaymentMethodService
) {

    @GetMapping
    fun getAllPaymentMethods(): Collection<PaymentMethodResponseDto> = paymentMethodService.getAllPaymentMethods()

    @PostMapping
    fun  createPaymentMethod(@RequestBody paymentMethodInsertDto: PaymentMethodInsertDto): PaymentMethodResponseDto =
        paymentMethodService.createPaymentMethod(paymentMethodInsertDto)

    @PutMapping
    fun  updatePaymentMethod(@RequestBody paymentMethodUpdateDto: PaymentMethodUpdateDto): PaymentMethodResponseDto =
        paymentMethodService.updatePaymentMethod(paymentMethodUpdateDto)

    @DeleteMapping("{id}")
    fun deletePaymentMethod(@PathVariable id: UUID?): ResponseEntity<HttpStatus> {
        paymentMethodService.deletePaymentMethod(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}