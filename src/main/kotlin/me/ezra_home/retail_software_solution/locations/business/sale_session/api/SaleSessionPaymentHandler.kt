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

    fun add(sessionId: UUID, dto: SaleSessionPaymentAddDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        saleSessionValidator.canAddPayments(session)
        val identity = if (session.originalStatus == SaleStatus.CONFIRMED) {
            val saleId = session.saleId ?: throw RtsGenericException("CONFIRMED session is missing a saleId")
            persistPayment(saleId, dto)
        } else {
            SessionIdentity.mintFreshIdentity()
        }
        val payment = SaleSessionPayment(
            identity = identity,
            paymentMethodId = dto.paymentMethodId,
            amount = dto.amount,
            reference = dto.reference,
            paymentDate = dto.paymentDate,
        )
        return saleSessionUpdateFinalizer.finalize(session.copy(payments = session.payments + payment))
    }

    private fun persistPayment(saleId: UUID,  dto: SaleSessionPaymentAddDto): SessionIdentity {
        val response = salePaymentService.recordPayment(
            SalePaymentCreateDto(
                saleId = saleId,
                paymentMethodId = dto.paymentMethodId,
                amount = dto.amount,
                reference = dto.reference,
                paymentDate = dto.paymentDate,
            )
        )
        return SessionIdentity.persisted(response.id)
    }

    fun remove(sessionId: UUID, dto: SaleSessionPaymentRemoveDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        val targetKey = dto.identity.key()
        val target = session.payments.firstOrNull { it.identity.key() == targetKey }
            ?: throw RtsGenericException("Payment not found on session")
        val persistedId = target.identity.id
        val refreshedPayments = if (persistedId == null) {
            saleSessionValidator.canDiscardPayments(session)
            session.payments.filter { it.identity.key() != targetKey }
        } else {
            val reason = dto.voidReason
                ?: throw RtsGenericException("voidReason is required when voiding a recorded payment")
            val response = salePaymentService.voidPayment(
                SalePaymentVoidCreateDto(salePaymentId = persistedId, reason = reason)
            )
            session.payments.map {
                if (it.identity.key() == targetKey) it.copy(voidedReason = response.voidedReason) else it
            }
        }
        return saleSessionUpdateFinalizer.finalize(session.copy(payments = refreshedPayments))
    }

}
