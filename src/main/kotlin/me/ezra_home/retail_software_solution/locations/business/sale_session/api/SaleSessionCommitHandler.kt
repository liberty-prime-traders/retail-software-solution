package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitInput
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitLine
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitOutcome
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitPayment
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleConfirmCommitter
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDraftCommitter
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionAssembler
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
    private val saleDraftCommitter: SaleDraftCommitter,
    private val saleConfirmCommitter: SaleConfirmCommitter,
) {

    fun saveDraft(sessionId: String): SaleSessionResponseDto {
        val session = loadAndValidate(sessionId)
        val input = toCommitInput(session)
        val outcome = saleDraftCommitter.saveDraft(input)
        val refreshed = applyOutcome(session, outcome)
        val recomputed = saleSessionTotalsCalculator.recompute(refreshed)
        saleSessionStore.save(recomputed)
        return saleSessionAssembler.buildResponse(recomputed)
    }

    fun confirm(sessionId: String): SaleSessionResponseDto {
        val session = loadAndValidate(sessionId)
        saleSessionValidator.guardWalkInFullyCovered(session)
        saleSessionValidator.guardPaymentsWithinTotal(session)
        val input = toCommitInput(session)
        val outcome = saleConfirmCommitter.confirm(input)
        val refreshed = applyOutcome(session, outcome)
        val response = saleSessionAssembler.buildResponse(refreshed)
        saleSessionStore.delete(sessionId)
        return response
    }

    private fun loadAndValidate(sessionId: String): SaleSession {
        val session = saleSessionStore.load(sessionId)
        val recomputed = saleSessionTotalsCalculator.recompute(session)
        saleSessionValidator.validate(recomputed)
        return recomputed
    }

    private fun toCommitInput(session: SaleSession): SaleCommitInput {
        return SaleCommitInput(
            saleId = session.saleId,
            expectedVersion = session.saleVersion,
            contactId = session.header.contactId,
            soldById = session.header.soldById,
            dateSold = session.header.dateSold,
            notes = session.header.notes,
            lines = session.lines.map { line ->
                SaleCommitLine(
                    clientKey = line.id.key(),
                    existingId = line.id.id,
                    locationProductId = line.locationProductId,
                    quantity = line.quantity,
                    unitId = line.unitId,
                    conversionFactor = line.conversionFactor,
                    unitPrice = line.unitPrice,
                )
            },
            adjustments = session.adjustments.map { adj ->
                SaleCommitAdjustment(
                    clientKey = adj.id.key(),
                    existingId = adj.id.id,
                    lineClientKey = adj.lineId?.key(),
                    adjustmentReasonId = adj.adjustmentReasonId,
                    direction = adj.direction,
                    calculationMethod = adj.calculationMethod,
                    value = adj.value,
                    note = adj.note,
                    approvedById = adj.approvedById,
                )
            },
            payments = session.payments.map { payment ->
                SaleCommitPayment(
                    clientKey = payment.id.key(),
                    existingId = payment.id.id,
                    paymentMethodId = payment.paymentMethodId,
                    amount = payment.amount,
                    reference = payment.reference,
                    paymentDate = payment.paymentDate,
                )
            },
        )
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
                val newId = outcome.lineIdsByClientKey[line.id.key()]
                if (newId != null && !line.id.isPersisted()) {
                    line.copy(id = SessionIdentity.persisted(newId))
                } else line
            },
            adjustments = session.adjustments.map { adj ->
                val newId = outcome.adjustmentIdsByClientKey[adj.id.key()]
                if (newId != null && !adj.id.isPersisted()) {
                    adj.copy(id = SessionIdentity.persisted(newId))
                } else adj
            },
            payments = session.payments.map { payment ->
                val newId = outcome.paymentIdsByClientKey[payment.id.key()]
                if (newId != null && !payment.id.isPersisted()) {
                    payment.copy(id = SessionIdentity.persisted(newId))
                } else payment
            },
        )
    }
}
