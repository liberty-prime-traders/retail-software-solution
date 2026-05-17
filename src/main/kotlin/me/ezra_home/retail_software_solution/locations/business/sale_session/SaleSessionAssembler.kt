package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.ActiveSessionUser
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustmentDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPaymentDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionSummaryDto
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonService
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class SaleSessionAssembler(
    private val contactService: ContactService,
    private val userQualifier: UserQualifier,
    private val paymentMethodService: PaymentMethodService,
    private val adjustmentReasonService: AdjustmentReasonService,
    private val saleSessionTotalsCalculator: SaleSessionTotalsCalculator,
    @Value("\${sale-session.active-warning-seconds:300}") private val activeWarningSeconds: Long,
) {

    fun buildResponse(session: SaleSession): SaleSessionResponseDto {
        val contactName = contactNameFor(session.header.contactId)
        val paymentMethodNames = paymentMethodService.getNamesById()
        val reasonNamesById = adjustmentReasonService.getReasonNamesById()
        return SaleSessionResponseDto(
            sessionId = session.sessionId,
            saleId = session.saleId,
            saleVersion = session.saleVersion,
            locationId = session.locationId,
            createdById = session.createdById,
            createdByLabel = userQualifier.getUserFullName(session.createdById),
            createdAt = session.createdAt,
            lastUpdatedAt = session.lastUpdatedAt,
            lastAccessedById = session.lastAccessedById,
            lastAccessedByLabel = userQualifier.getUserFullName(session.lastAccessedById),
            lastAccessedAt = session.lastAccessedAt,
            contactId = session.header.contactId,
            contactLabel = contactName,
            walkInCustomer = session.header.contactId == SystemContact.WALK_IN.id,
            soldById = session.header.soldById,
            soldByLabel = userQualifier.getUserFullName(session.header.soldById),
            dateSold = session.header.dateSold,
            notes = session.header.notes,
            lines = session.lines.map { line ->
                SaleSessionLineDto(
                    id = line.id,
                    locationProductId = line.locationProductId,
                    productLabel = line.productLabel,
                    quantity = line.quantity,
                    unitId = line.unitId,
                    conversionFactor = line.conversionFactor,
                    unitPrice = line.unitPrice,
                    lineTotal = line.lineTotal(),
                )
            },
            adjustments = session.adjustments.map { adj ->
                SaleSessionAdjustmentDto(
                    id = adj.id,
                    lineId = adj.lineId,
                    adjustmentReasonId = adj.adjustmentReasonId,
                    adjustmentReasonLabel = reasonNamesById[adj.adjustmentReasonId],
                    direction = adj.direction,
                    calculationMethod = adj.calculationMethod,
                    value = adj.value,
                    calculatedAmount = saleSessionTotalsCalculator.calculatedAmount(adj, session.lines),
                    note = adj.note,
                    approvedById = adj.approvedById,
                    approvedByLabel = userQualifier.getUserFullName(adj.approvedById),
                )
            },
            payments = session.payments.map { payment ->
                SaleSessionPaymentDto(
                    id = payment.id,
                    paymentMethodId = payment.paymentMethodId,
                    paymentMethodLabel = paymentMethodNames[payment.paymentMethodId],
                    amount = payment.amount,
                    reference = payment.reference,
                    paymentDate = payment.paymentDate,
                )
            },
            totals = session.totals,
        )
    }

    fun buildSummaries(sessions: List<SaleSession>): List<SaleSessionSummaryDto> {
        if (sessions.isEmpty()) return emptyList()
        val now = DateTimes.Offset.Now.organization()
        val warning = Duration.ofSeconds(activeWarningSeconds)
        return sessions.map { session ->
            val contactName = contactNameFor(session.header.contactId)
            val activeUser = if (Duration.between(session.lastAccessedAt, now) <= warning) {
                ActiveSessionUser(
                    userId = session.lastAccessedById,
                    userLabel = userQualifier.getUserFullName(session.lastAccessedById),
                    lastSeenAt = session.lastAccessedAt,
                )
            } else null
            SaleSessionSummaryDto(
                sessionId = session.sessionId,
                createdById = session.createdById,
                createdByLabel = userQualifier.getUserFullName(session.createdById),
                createdAt = session.createdAt,
                lastUpdatedAt = session.lastUpdatedAt,
                contactId = session.header.contactId,
                contactLabel = contactName,
                saleId = session.saleId,
                lineCount = session.lines.size,
                payableTotal = session.totals.payableTotal,
                activeUser = activeUser,
            )
        }
    }

    private fun contactNameFor(contactId: UUID): String {
        if (contactId == SystemContact.WALK_IN.id) return "Walk-In Customer"
        return contactService.getContactById(contactId).identity.displayName
    }
}
