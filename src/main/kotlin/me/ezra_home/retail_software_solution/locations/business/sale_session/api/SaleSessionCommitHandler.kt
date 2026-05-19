package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitOutcome
import me.ezra_home.retail_software_solution.locations.business.sale.api.ConfirmedSalePersister
import me.ezra_home.retail_software_solution.locations.business.sale.api.DraftSalePersister
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionAssembler
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionCommitMapper
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionTotalsCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleSessionCommitHandler(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionAssembler: SaleSessionAssembler,
    private val saleSessionValidator: SaleSessionValidator,
    private val saleSessionTotalsCalculator: SaleSessionTotalsCalculator,
    private val saleSessionCommitMapper: SaleSessionCommitMapper,
    private val draftSalePersister: DraftSalePersister,
    private val confirmedSalePersister: ConfirmedSalePersister,
) {

    fun saveDraft(sessionId: UUID): SaleSessionResponseDto {
        val session = loadAndValidate(sessionId)
        val input = saleSessionCommitMapper.toCommitInput(session)
        val outcome = draftSalePersister.saveDraft(input)
        val refreshed = applyOutcome(session, outcome)
        val recomputed = saleSessionTotalsCalculator.recompute(refreshed)
        saleSessionStore.save(recomputed)
        return saleSessionAssembler.buildResponse(recomputed)
    }

    fun confirm(sessionId: UUID): SaleSessionResponseDto {
        val session = loadAndValidate(sessionId)
        saleSessionValidator.guardNonEmptyLines(session)
        saleSessionValidator.guardWalkInFullyCovered(session)
        saleSessionValidator.guardPaymentsWithinTotal(session)
        val input = saleSessionCommitMapper.toCommitInput(session)
        val outcome = confirmedSalePersister.confirm(input)
        val refreshed = applyOutcome(session, outcome)
        val response = saleSessionAssembler.buildResponse(refreshed)
        saleSessionStore.delete(sessionId)
        return response
    }

    private fun loadAndValidate(sessionId: UUID): SaleSession {
        val session = saleSessionStore.load(sessionId)
        saleSessionValidator.guardMutable(session)
        val recomputed = saleSessionTotalsCalculator.recompute(session)
        saleSessionValidator.validate(recomputed)
        return recomputed
    }

    private fun applyOutcome(session: SaleSession, outcome: SaleCommitOutcome): SaleSession {
        val now = DateTimes.Offset.Now.organization()
        val userId = SessionContextProvider.getUserId()
        return session.copy(
            saleId = outcome.saleId,
            saleVersion = outcome.newVersion,
            lastUpdatedAt = now,
            lastAccessedAt = now,
            lastAccessedById = userId,
            lines = session.lines.map { line ->
                val newId = outcome.lineIdsByClientKey[line.identity.key()]
                if (newId != null && !line.identity.isPersisted()) {
                    line.copy(identity = SessionIdentity.persisted(newId))
                } else line
            },
            adjustments = session.adjustments.map { adj ->
                val newId = outcome.adjustmentIdsByClientKey[adj.identity.key()]
                if (newId != null && !adj.identity.isPersisted()) {
                    adj.copy(identity = SessionIdentity.persisted(newId))
                } else adj
            },
            payments = session.payments.map { payment ->
                val newId = outcome.paymentIdsByClientKey[payment.identity.key()]
                if (newId != null && !payment.identity.isPersisted()) {
                    payment.copy(identity = SessionIdentity.persisted(newId))
                } else payment
            },
        )
    }
}
