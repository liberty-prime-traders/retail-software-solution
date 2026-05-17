package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionAssembler
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionTotalsCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
class SaleSessionPaymentHandler(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionAssembler: SaleSessionAssembler,
    private val saleSessionValidator: SaleSessionValidator,
    private val saleSessionTotalsCalculator: SaleSessionTotalsCalculator,
) {

    fun add(sessionId: String, dto: SaleSessionPaymentAddDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        val payment = SaleSessionPayment(
            id = SessionIdentity.fresh(),
            paymentMethodId = dto.paymentMethodId,
            amount = dto.amount,
            reference = dto.reference,
            paymentDate = dto.paymentDate,
        )
        return finish(session.copy(payments = session.payments + payment))
    }

    fun remove(sessionId: String, dto: SaleSessionRowIdentityDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        val targetKey = dto.id.key()
        val survivors = session.payments.filter { it.id.key() != targetKey }
        if (survivors.size == session.payments.size) {
            throw RtsGenericException("Payment not found on session")
        }
        return finish(session.copy(payments = survivors))
    }

    private fun finish(updated: SaleSession): SaleSessionResponseDto {
        val now = DateTimes.Offset.Now.organization()
        val touched = updated.touched(SessionContextProvider.getUserId(), now)
        val withTotals = saleSessionTotalsCalculator.recompute(touched)
        saleSessionValidator.validate(withTotals)
        saleSessionStore.save(withTotals)
        return saleSessionAssembler.buildResponse(withTotals)
    }
}
