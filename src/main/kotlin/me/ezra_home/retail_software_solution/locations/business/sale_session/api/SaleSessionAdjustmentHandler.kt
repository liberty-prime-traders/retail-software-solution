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
    fun add(sessionId: UUID, adjustmentAddDto: SaleSessionAdjustmentAddDto): SaleSessionResponseDto {
        val saleSession = saleSessionStore.load(sessionId)
        saleSessionValidator.guardMutable(saleSession)
        if (adjustmentAddDto.relatedSaleLineIdentity != null) {
            val targetLineKey = adjustmentAddDto.relatedSaleLineIdentity.key()
            if (saleSession.saleLines.none { it.identity.key() == targetLineKey }) {
                throw RtsGenericException("Adjustment references a line that is not on the sale")
            }
        }
        val newSaleSessionAdjustment = SaleSessionAdjustment(
            identity = SessionIdentity.mintFreshIdentity(),
            relatedSaleLineIdentity = adjustmentAddDto.relatedSaleLineIdentity,
            adjustmentReasonId = adjustmentAddDto.adjustmentReasonId,
            direction = adjustmentAddDto.direction,
            calculationMethod = adjustmentAddDto.calculationMethod,
            value = adjustmentAddDto.value,
            note = adjustmentAddDto.note,
            approvedById = adjustmentAddDto.approvedById,
        )
        val updatedSaleSession = saleSession.copy(saleAdjustments = saleSession.saleAdjustments + newSaleSessionAdjustment)
        return saleSessionUpdateFinalizer.finalize(updatedSaleSession)
    }

    fun remove(sessionId: UUID, rowIdentityDto: SaleSessionRowIdentityDto): SaleSessionResponseDto {
        val saleSession = saleSessionStore.load(sessionId)
        saleSessionValidator.guardMutable(saleSession)
        val targetAdjustmentKey = rowIdentityDto.identity.key()
        val survivingSaleAdjustments = saleSession.saleAdjustments.filter { it.identity.key() != targetAdjustmentKey }
        if (survivingSaleAdjustments.size == saleSession.saleAdjustments.size) {
            throw RtsGenericException("Adjustment not found on session")
        }
        val updatedSaleSession = saleSession.copy(saleAdjustments = survivingSaleAdjustments)
        return saleSessionUpdateFinalizer.finalize(updatedSaleSession)
    }

}
