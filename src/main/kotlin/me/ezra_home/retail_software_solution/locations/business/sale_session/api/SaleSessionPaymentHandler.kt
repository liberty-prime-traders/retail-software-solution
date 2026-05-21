package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import java.util.UUID

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentService
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentVoidCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionUpdateFinalizer
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
class SaleSessionPaymentHandler(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionValidator: SaleSessionValidator,
    private val saleSessionUpdateFinalizer: SaleSessionUpdateFinalizer,
    private val salePaymentService: SalePaymentService,
) {

    fun add(sessionId: UUID, paymentAddDto: SaleSessionPaymentAddDto): SaleSessionResponseDto {
        val saleSession = saleSessionStore.load(sessionId)
        saleSessionValidator.canAddPayments(saleSession)
        val paymentIdentity = if (saleSession.originalStatus == SaleStatus.CONFIRMED) {
            val saleId = saleSession.saleId ?: throw RtsGenericException("CONFIRMED session is missing a saleId")
            persistPayment(saleId, paymentAddDto)
        } else {
            SessionIdentity.mintFreshIdentity()
        }
        val newSaleSessionPayment = SaleSessionPayment(
            identity = paymentIdentity,
            paymentMethodId = paymentAddDto.paymentMethodId,
            amount = paymentAddDto.amount,
            reference = paymentAddDto.reference,
            paymentDate = paymentAddDto.paymentDate,
        )
        return saleSessionUpdateFinalizer.finalize(
            saleSession.copy(salePayments = saleSession.salePayments + newSaleSessionPayment)
        )
    }

    private fun persistPayment(saleId: UUID, paymentAddDto: SaleSessionPaymentAddDto): SessionIdentity {
        val recordPaymentResponse = salePaymentService.recordPayment(
            SalePaymentCreateDto(
                saleId = saleId,
                paymentMethodId = paymentAddDto.paymentMethodId,
                amount = paymentAddDto.amount,
                reference = paymentAddDto.reference,
                paymentDate = paymentAddDto.paymentDate,
            )
        )
        return SessionIdentity.persisted(recordPaymentResponse.id)
    }

    fun remove(sessionId: UUID, paymentRemoveDto: SaleSessionPaymentRemoveDto): SaleSessionResponseDto {
        val saleSession = saleSessionStore.load(sessionId)
        val targetPaymentKey = paymentRemoveDto.identity.key()
        val targetSaleSessionPayment = saleSession.salePayments.firstOrNull { it.identity.key() == targetPaymentKey }
            ?: throw RtsGenericException("Payment not found on session")
        val persistedPaymentId = targetSaleSessionPayment.identity.id
        val refreshedSalePayments = if (persistedPaymentId == null) {
            saleSession.salePayments.filter { it.identity.key() != targetPaymentKey }
        } else {
            val voidReason = paymentRemoveDto.voidReason
                ?: throw RtsGenericException("voidReason is required when voiding a recorded payment")
            val voidPaymentResponse = salePaymentService.voidPayment(
                SalePaymentVoidCreateDto(salePaymentId = persistedPaymentId, reason = voidReason)
            )
            saleSession.salePayments.map { saleSessionPayment ->
                if (saleSessionPayment.identity.key() == targetPaymentKey) {
                    saleSessionPayment.copy(voidedReason = voidPaymentResponse.voidedReason)
                } else saleSessionPayment
            }
        }
        return saleSessionUpdateFinalizer.finalize(saleSession.copy(salePayments = refreshedSalePayments))
    }

}
