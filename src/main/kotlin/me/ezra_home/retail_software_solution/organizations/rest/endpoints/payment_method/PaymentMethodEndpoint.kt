package me.ezra_home.retail_software_solution.organizations.rest.endpoints.payment_method

import me.ezra_home.retail_software_solution.organizations.business.payment_method.PaymentMethodService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.dto.PaymentMethodInsertDto
import me.ezra_home.retail_software_solution.organizations.business.payment_method.dto.PaymentMethodResponseDto
import me.ezra_home.retail_software_solution.organizations.business.payment_method.dto.PaymentMethodUpdateDto
import org.springframework.http.HttpStatus
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
import java.util.UUID

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
