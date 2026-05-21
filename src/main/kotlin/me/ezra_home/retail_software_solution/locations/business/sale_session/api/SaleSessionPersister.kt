package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSaveResult
import me.ezra_home.retail_software_solution.locations.business.sale.api.ConfirmedSalePersister
import me.ezra_home.retail_software_solution.locations.business.sale.api.DraftSalePersister
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionAssembler
import me.ezra_home.retail_software_solution.locations.business.sale_session.SessionToSaveRequestMapper
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionTotalsCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleSessionPersister(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionAssembler: SaleSessionAssembler,
    private val saleSessionValidator: SaleSessionValidator,
    private val saleSessionTotalsCalculator: SaleSessionTotalsCalculator,
    private val sessionToSaveRequestMapper: SessionToSaveRequestMapper,
    private val draftSalePersister: DraftSalePersister,
    private val confirmedSalePersister: ConfirmedSalePersister,
) {

    fun saveDraft(sessionId: UUID): SaleSessionResponseDto {
        val saleSession = loadAndValidate(sessionId)
        val saleSaveRequest = sessionToSaveRequestMapper.toSaleSaveRequest(saleSession)
        val saleSaveResult = draftSalePersister.saveDraft(saleSaveRequest)
        val saleSessionAfterSave = applySaleSaveResult(saleSession, saleSaveResult)
        val saleSessionWithTotals = saleSessionTotalsCalculator.recompute(saleSessionAfterSave)
        saleSessionStore.save(saleSessionWithTotals)
        return saleSessionAssembler.buildResponse(saleSessionWithTotals)
    }

    fun confirm(sessionId: UUID): SaleSessionResponseDto {
        val saleSession = loadAndValidate(sessionId)
        saleSessionValidator.guardNonEmptyLines(saleSession)
        saleSessionValidator.guardWalkInFullyCovered(saleSession)
        saleSessionValidator.guardPaymentsWithinTotal(saleSession)
        val saleSaveRequest = sessionToSaveRequestMapper.toSaleSaveRequest(saleSession)
        val saleSaveResult = confirmedSalePersister.confirm(saleSaveRequest)
        val saleSessionAfterSave = applySaleSaveResult(saleSession, saleSaveResult)
        val saleSessionResponseDto = saleSessionAssembler.buildResponse(saleSessionAfterSave)
        saleSessionStore.delete(sessionId)
        return saleSessionResponseDto
    }

    private fun loadAndValidate(sessionId: UUID): SaleSession {
        val saleSession = saleSessionStore.load(sessionId)
        saleSessionValidator.guardMutable(saleSession)
        val saleSessionWithTotals = saleSessionTotalsCalculator.recompute(saleSession)
        saleSessionValidator.validate(saleSessionWithTotals)
        return saleSessionWithTotals
    }

    private fun applySaleSaveResult(saleSession: SaleSession, saleSaveResult: SaleSaveResult): SaleSession {
        val now = DateTimes.Offset.Now.organization()
        val userId = SessionContextProvider.getUserId()
        return saleSession.copy(
            saleId = saleSaveResult.saleId,
            saleVersion = saleSaveResult.newVersion,
            lastUpdatedAt = now,
            lastAccessedAt = now,
            lastAccessedById = userId,
            saleLines = saleSession.saleLines.map { saleSessionLine ->
                val newSaleLineId = saleSaveResult.saleLineIdsByClientKey[saleSessionLine.identity.key()]
                if (newSaleLineId != null && !saleSessionLine.identity.isPersisted()) {
                    saleSessionLine.copy(identity = SessionIdentity.persisted(newSaleLineId))
                } else saleSessionLine
            },
            saleAdjustments = saleSession.saleAdjustments.map { saleSessionAdjustment ->
                val newSaleAdjustmentId = saleSaveResult.saleAdjustmentIdsByClientKey[saleSessionAdjustment.identity.key()]
                if (newSaleAdjustmentId != null && !saleSessionAdjustment.identity.isPersisted()) {
                    saleSessionAdjustment.copy(identity = SessionIdentity.persisted(newSaleAdjustmentId))
                } else saleSessionAdjustment
            },
            salePayments = saleSession.salePayments.map { saleSessionPayment ->
                val newSalePaymentId = saleSaveResult.salePaymentIdsByClientKey[saleSessionPayment.identity.key()]
                if (newSalePaymentId != null && !saleSessionPayment.identity.isPersisted()) {
                    saleSessionPayment.copy(identity = SessionIdentity.persisted(newSalePaymentId))
                } else saleSessionPayment
            },
        )
    }
}
