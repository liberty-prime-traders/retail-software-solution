package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionAssembler
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionTotalsCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
class SaleSessionAdjustmentHandler(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionAssembler: SaleSessionAssembler,
    private val saleSessionValidator: SaleSessionValidator,
    private val saleSessionTotalsCalculator: SaleSessionTotalsCalculator,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun add(sessionId: String, dto: SaleSessionAdjustmentAddDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        if (dto.lineId != null) {
            val targetKey = dto.lineId.key()
            if (session.lines.none { it.id.key() == targetKey }) {
                throw RtsGenericException("Adjustment references a line that is not on the sale")
            }
        }
        val adjustment = SaleSessionAdjustment(
            id = SessionIdentity.fresh(),
            lineId = dto.lineId,
            adjustmentReasonId = dto.adjustmentReasonId,
            direction = dto.direction,
            calculationMethod = dto.calculationMethod,
            value = dto.value,
            note = dto.note,
            approvedById = dto.approvedById,
        )
        val updated = session.copy(adjustments = session.adjustments + adjustment)
        return finish(updated)
    }

    fun remove(sessionId: String, dto: SaleSessionRowIdentityDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        val targetKey = dto.id.key()
        val survivors = session.adjustments.filter { it.id.key() != targetKey }
        if (survivors.size == session.adjustments.size) {
            throw RtsGenericException("Adjustment not found on session")
        }
        val updated = session.copy(adjustments = survivors)
        return finish(updated)
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
