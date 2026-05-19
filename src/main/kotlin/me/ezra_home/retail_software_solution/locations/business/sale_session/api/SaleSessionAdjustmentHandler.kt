package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import java.util.UUID

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionUpdateFinalizer
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
class SaleSessionAdjustmentHandler(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionValidator: SaleSessionValidator,
    private val saleSessionUpdateFinalizer: SaleSessionUpdateFinalizer,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun add(sessionId: UUID, dto: SaleSessionAdjustmentAddDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        saleSessionValidator.guardMutable(session)
        if (dto.lineIdentity != null) {
            val targetKey = dto.lineIdentity.key()
            if (session.lines.none { it.identity.key() == targetKey }) {
                throw RtsGenericException("Adjustment references a line that is not on the sale")
            }
        }
        val adjustment = SaleSessionAdjustment(
            identity = SessionIdentity.mintFreshIdentity(),
            lineIdentity = dto.lineIdentity,
            adjustmentReasonId = dto.adjustmentReasonId,
            direction = dto.direction,
            calculationMethod = dto.calculationMethod,
            value = dto.value,
            note = dto.note,
            approvedById = dto.approvedById,
        )
        val updated = session.copy(adjustments = session.adjustments + adjustment)
        return saleSessionUpdateFinalizer.finalize(updated)
    }

    fun remove(sessionId: UUID, dto: SaleSessionRowIdentityDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        saleSessionValidator.guardMutable(session)
        val targetKey = dto.identity.key()
        val survivors = session.adjustments.filter { it.identity.key() != targetKey }
        if (survivors.size == session.adjustments.size) {
            throw RtsGenericException("Adjustment not found on session")
        }
        val updated = session.copy(adjustments = survivors)
        return saleSessionUpdateFinalizer.finalize(updated)
    }

}
